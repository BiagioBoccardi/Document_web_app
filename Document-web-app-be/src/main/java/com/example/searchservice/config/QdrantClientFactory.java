package com.example.searchservice.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.concurrent.TimeUnit;

public class QdrantClientFactory {

    private static final String QDRANT_HOST = DotenvConfig.get("QDRANT_HOST", "localhost");
    private static final int QDRANT_PORT = DotenvConfig.getAsInt("QDRANT_PORT", 6333);

    public static QdrantClient createClient() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(QDRANT_HOST, QDRANT_PORT)
                .usePlaintext()
                .build();

        return new QdrantClient(
                QdrantGrpcClient.newBuilder(channel)
                        .withTimeout(5, TimeUnit.SECONDS)
                        .build());
    }

    public static void closeClient(QdrantClient client) {
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception e) {
            System.err.println("Errore durante la chiusura del client Qdrant: " + e.getMessage());
        }
    }
}
