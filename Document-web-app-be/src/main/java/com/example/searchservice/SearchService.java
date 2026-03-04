package com.example.searchservice;

import com.example.searchservice.config.DotenvConfig;
import com.example.searchservice.config.QdrantClientFactory;
import com.example.searchservice.embedding.EmbeddingProvider;
import com.example.searchservice.embedding.EmbeddingProviderFactory;
import io.javalin.Javalin;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;

import java.util.concurrent.ExecutionException;

public class SearchService {

    private static final String QDRANT_COLLECTION_NAME = "documents";

    public static void main(String[] args) {
        int port = DotenvConfig.getAsInt("SEARCH_SERVICE_PORT", 83);

        QdrantClient qdrantClient = QdrantClientFactory.createClient();
        setupQdrantCollection(qdrantClient); // CONFIGURAZIONE COLLEZIONE QDRANT

        EmbeddingProvider embeddingProvider = EmbeddingProviderFactory.createProvider();

        Javalin app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.showJavalinBanner = false;
        }).start(port);

        System.out.printf("--- Search Service avviato sulla porta: %d ---%n", port);

        app.get("/health", ctx -> {
            ctx.json("{\"status\": "UP"}");
            // Aggiungeremo un controllo anche per Qdrant qui in futuro
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
            // Controlliamo se la collezione esiste già
            boolean collectionExists = client.getCollectionsList().get().getCollectionsList().stream()
                    .anyMatch(collection -> collection.getName().equals(QDRANT_COLLECTION_NAME));

            if (!collectionExists) {
                System.out.printf("La collezione '%s' non esiste. Creazione in corso...%n", QDRANT_COLLECTION_NAME);

                // Creiamo la collezione specificando la dimensione dei vettori e la metrica di distanza
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
            // Termina l'applicazione se Qdrant non è raggiungibile o configurabile.
            System.exit(1);
        }
    }
}
