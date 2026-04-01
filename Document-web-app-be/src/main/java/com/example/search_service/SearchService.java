package com.example.search_service;

import java.util.concurrent.ExecutionException;

import com.example.search_service.auth.JwtAuthMiddleware;
import com.example.search_service.config.DotenvConfig;
import com.example.search_service.config.QdrantClientFactory;
import com.example.search_service.config.RabbitMQConfig;
import com.example.search_service.controller.SearchController;
import com.example.search_service.dto.ErrorResponse;
import com.example.search_service.embedding.EmbeddingProvider;
import com.example.search_service.embedding.EmbeddingProviderFactory;
import com.example.search_service.messaging.DocumentEventConsumer;

import com.rabbitmq.client.Connection;
import io.javalin.Javalin;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchService {

    private static final String QDRANT_COLLECTION_NAME = "documents";

    public static void main(String[] args) {
        int port = DotenvConfig.getAsInt("SEARCH_SERVICE_PORT", 83);

        // Inizializzazione Qdrant
        QdrantClient qdrantClient = QdrantClientFactory.createClient();
        setupQdrantCollection(qdrantClient);

        // Inizializzazione provider embedding
        EmbeddingProvider embeddingProvider = EmbeddingProviderFactory.createProvider();

        // Inizializzazione RabbitMQ e consumer
        Connection rabbitConnection = RabbitMQConfig.createConnection();
        DocumentEventConsumer eventConsumer =
                new DocumentEventConsumer(embeddingProvider, qdrantClient);
        try {
            eventConsumer.startConsuming(rabbitConnection);
        } catch (Exception e) {
            log.error("Errore durante l'avvio dei consumer RabbitMQ: {}", e.getMessage());
        }

        // Inizializzazione controller HTTP
        SearchController searchController = new SearchController(embeddingProvider, qdrantClient);

        // Configurazione Javalin
        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.showJavalinBanner = false;
        }).start(port);

        // Gestore globale eccezioni non gestite (SS-BE-13)
        app.exception(Exception.class, (e, ctx) -> {
            log.error("Eccezione non gestita: {}", e.getMessage(), e);
            ctx.status(500).json(new ErrorResponse(500, "Errore interno del server"));
        });

        // Middleware JWT — protegge tutte le rotte /api/v1/*
        app.before("/api/v1/*", JwtAuthMiddleware::handle);

        // Registrazione rotte HTTP
        searchController.registerRoutes(app);

        // Health check (non protetto)
        app.get("/health", ctx -> ctx.json("{\"status\": \"UP\"}"));

        log.info("--- Search Service avviato sulla porta: {} ---", port);

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("--- Chiusura del Search Service ---");
            RabbitMQConfig.closeConnection();
            QdrantClientFactory.closeClient(qdrantClient);
            app.stop();
            log.info("--- Servizio arrestato ---");
        }));
    }

    private static void setupQdrantCollection(QdrantClient client) {
        final int vectorSize = 384;

        try {
            boolean collectionExists = client.listCollectionsAsync().get()
                    .contains(QDRANT_COLLECTION_NAME);

            if (!collectionExists) {
                log.info("La collezione '{}' non esiste. Creazione in corso...",
                        QDRANT_COLLECTION_NAME);

                client.createCollectionAsync(QDRANT_COLLECTION_NAME,
                        VectorParams.newBuilder()
                                .setSize(vectorSize)
                                .setDistance(Distance.Cosine)
                                .build()
                ).get();

                log.info("Collezione '{}' creata con successo in Qdrant",
                        QDRANT_COLLECTION_NAME);
            } else {
                log.info("Collezione '{}' già esistente in Qdrant",
                        QDRANT_COLLECTION_NAME);
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Errore durante la configurazione della collezione Qdrant: {}",
                    e.getMessage());
            System.exit(1);
        }
    }
}