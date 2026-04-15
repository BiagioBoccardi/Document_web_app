package com.example.search_service.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.time.Duration;

public class QdrantClientFactory {

    private static final String QDRANT_HOST = DotenvConfig.get("QDRANT_HOST", "localhost");
    private static final int QDRANT_PORT = DotenvConfig.getAsInt("QDRANT_PORT", 6333);

    public static QdrantClient createClient() {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(QDRANT_HOST);
            String resolvedHost = QDRANT_HOST;
            for (InetAddress addr : addresses) {
                if (addr instanceof Inet4Address) {
                    resolvedHost = addr.getHostAddress();
                    break;
                }
            }
            System.out.println(">>> Qdrant connecting to: " + resolvedHost + ":" + QDRANT_PORT);

            ManagedChannel channel = ManagedChannelBuilder
                .forTarget("dns:///" + resolvedHost + ":" + QDRANT_PORT)
                .usePlaintext()
                .build();

            return new QdrantClient(
                QdrantGrpcClient.newBuilder(channel)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Errore connessione Qdrant: " + e.getMessage(), e);
        }
    }

    public static void closeClient(QdrantClient client) {
        try {
            if (client != null) client.close();
        } catch (Exception e) {
            System.err.println("Errore chiusura client Qdrant: " + e.getMessage());
        }
    }
}