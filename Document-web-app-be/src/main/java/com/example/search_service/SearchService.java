package com.example.search_service;

import java.util.concurrent.ExecutionException;

import com.example.search_service.auth.JwtAuthMiddleware;
import com.example.search_service.config.DotenvConfig;
import com.example.search_service.config.QdrantClientFactory;
import com.example.search_service.embedding.EmbeddingProvider;
import com.example.search_service.embedding.EmbeddingProviderFactory; // Importazione del middleware

import io.javalin.Javalin;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;

public class SearchService {

    private static final String QDRANT_COLLECTION_NAME = "documents";

    public static void main(String[] args) {
        int port = DotenvConfig.getAsInt("SEARCH_SERVICE_PORT", 83);

        QdrantClient qdrantClient = QdrantClientFactory.createClient();
        setupQdrantCollection(qdrantClient); // CONFIGURAZIONE COLLEZIONE QDRANT

        // Istanza preparata per i prossimi endpoint (SS-BE-04 / SS-BE-05)
        EmbeddingProvider embeddingProvider = EmbeddingProviderFactory.createProvider();

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.showJavalinBanner = false;
        }).start(port);

        // --- ATTIVAZIONE MIDDLEWARE JWT (SS-BE-03) ---
        // Protegge tutte le rotte sotto /api/v1/ estraendo il userId dal token
        app.before("/api/v1/*", JwtAuthMiddleware::handle);

        System.out.printf("--- Search Service avviato sulla porta: %d ---%n", port);

        // Endpoint LIBERO (non protetto perché non inizia con /api/v1/)
        app.get("/health", ctx -> {
            ctx.json("{\"status\": \"UP\"}");
            // Aggiungeremo un controllo anche per Qdrant qui in futuro
        });

        // Esempio di endpoint PROTETTO (richiederà il token)
        app.get("/api/v1/test-auth", ctx -> {
            String userId = ctx.attribute("userId");
            ctx.result("Token valido! Benvenuto utente: " + userId);
        });

        // Gestione della chiusura delle risorse
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("--- Chiusura del Search Service ---");
            QdrantClientFactory.closeClient(qdrantClient);
            app.stop();
            System.out.println("--- Servizio arrestato ---");
        }));
    }

    /**
     * Controlla l'esistenza della collezione Qdrant e la crea se non presente.
     * @param client Il client Qdrant da utilizzare.
     */
    private static void setupQdrantCollection(QdrantClient client) {
        final int vectorSize = 384; // Dimensione tipica per modelli come all-MiniLM-L6-v2

        try {
            boolean collectionExists = client.listCollectionsAsync().get().contains(QDRANT_COLLECTION_NAME);

            if (!collectionExists) {
                System.out.printf("La collezione '%s' non esiste. Creazione in corso...%n", QDRANT_COLLECTION_NAME);

                client.createCollectionAsync(QDRANT_COLLECTION_NAME,
                        VectorParams.newBuilder().setSize(vectorSize).setDistance(Distance.Cosine).build()
                ).get();

                System.out.printf("--- Collezione '%s' creata con successo in Qdrant ---%n", QDRANT_COLLECTION_NAME);
            } else {
                System.out.printf("--- Collezione '%s' già esistente in Qdrant ---%n", QDRANT_COLLECTION_NAME);
            }

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("ERRORE: Impossibile configurare la collezione Qdrant. Il servizio non può partire.");
            System.err.println("Dettagli errore: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}