package com.example.search_service;

import java.util.concurrent.ExecutionException;

import com.example.search_service.auth.JwtAuthMiddleware;
import com.example.search_service.config.DotenvConfig;
import com.example.search_service.config.QdrantClientFactory;
import com.example.search_service.embedding.EmbeddingProvider;
import com.example.search_service.embedding.EmbeddingProviderFactory;

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

        QdrantClient qdrantClient = QdrantClientFactory.createClient();
        setupQdrantCollection(qdrantClient);

        EmbeddingProvider embeddingProvider = EmbeddingProviderFactory.createProvider();

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.showJavalinBanner = false;
        }).start(port);

        app.before("/api/v1/*", JwtAuthMiddleware::handle);

        log.info("--- Search Service avviato sulla porta: {} ---", port);

        app.get("/health", ctx -> {
            ctx.json("{\"status\": \"UP\"}");
        });

        app.get("/api/v1/test-auth", ctx -> {
            String userId = ctx.attribute("userId");
            ctx.result("Token valido! Benvenuto utente: " + userId);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("--- Chiusura del Search Service ---");
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

                log.info("--- Collezione '{}' creata con successo in Qdrant ---",
                        QDRANT_COLLECTION_NAME);
            } else {
                log.info("--- Collezione '{}' già esistente in Qdrant ---",
                        QDRANT_COLLECTION_NAME);
            }

        } catch (InterruptedException | ExecutionException e) {
            log.error("Errore durante la configurazione della collezione Qdrant: {}",
                    e.getMessage());
            System.exit(1);
        }
    }
}