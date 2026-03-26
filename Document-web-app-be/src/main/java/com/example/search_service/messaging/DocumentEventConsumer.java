package com.example.search_service.messaging;

import com.example.search_service.config.RabbitMQConfig;
import com.example.search_service.config.ResilienceConfig;
import com.example.search_service.dto.DocumentEvent;
import com.example.search_service.embedding.EmbeddingProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;
import io.github.resilience4j.retry.Retry;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointStruct;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

/**
 * Consumer RabbitMQ per gli eventi documento.
 * Implementa Resilienza (Retry, Ack Manuale, Reject).
 */
@Slf4j
public class DocumentEventConsumer {

    private static final String COLLECTION_NAME = "documents";

    private final EmbeddingProvider embeddingProvider;
    private final QdrantClient qdrantClient;
    private final ObjectMapper objectMapper;

    public DocumentEventConsumer(EmbeddingProvider embeddingProvider, QdrantClient qdrantClient) {
        this.embeddingProvider = embeddingProvider;
        this.qdrantClient = qdrantClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Avvia i consumer per tutte e tre le code documento.
     */
    public void startConsuming(Connection connection) throws IOException {

        // SS-BE-09: document.uploaded
        registerConsumer(connection, RabbitMQConfig.QUEUE_DOCUMENT_UPLOADED, (channel, tag, delivery) -> {
            String body = new String(delivery.getBody());
            log.info("Evento ricevuto su '{}': {}", RabbitMQConfig.QUEUE_DOCUMENT_UPLOADED, body);
            try {
                DocumentEvent event = objectMapper.readValue(body, DocumentEvent.class);
                handleDocumentUploaded(event);
                // Conferma esplicita del successo a RabbitMQ
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                log.error("Fallimento definitivo elaborazione document.uploaded, scarto messaggio: {}", e.getMessage());
                // Scarto il messaggio (requeue=false). Lo invia alla DLQ se configurata.
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
            }
        });

        // SS-BE-10: document.updated
        registerConsumer(connection, RabbitMQConfig.QUEUE_DOCUMENT_UPDATED, (channel, tag, delivery) -> {
            String body = new String(delivery.getBody());
            log.info("Evento ricevuto su '{}': {}", RabbitMQConfig.QUEUE_DOCUMENT_UPDATED, body);
            try {
                DocumentEvent event = objectMapper.readValue(body, DocumentEvent.class);
                handleDocumentUpdated(event);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                log.error("Fallimento definitivo elaborazione document.updated, scarto messaggio: {}", e.getMessage());
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
            }
        });

        // SS-BE-11: document.deleted
        registerConsumer(connection, RabbitMQConfig.QUEUE_DOCUMENT_DELETED, (channel, tag, delivery) -> {
            String body = new String(delivery.getBody());
            log.info("Evento ricevuto su '{}': {}", RabbitMQConfig.QUEUE_DOCUMENT_DELETED, body);
            try {
                DocumentEvent event = objectMapper.readValue(body, DocumentEvent.class);
                handleDocumentDeleted(event);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            } catch (Exception e) {
                log.error("Fallimento definitivo elaborazione document.deleted, scarto messaggio: {}", e.getMessage());
                channel.basicReject(delivery.getEnvelope().getDeliveryTag(), false);
            }
        });

        log.info("Consumer RabbitMQ avviati (Ack manuale e Retry attivi)");
    }

    // ─────────────────────────────────────────────
    // SS-BE-09: document.uploaded
    // ─────────────────────────────────────────────
    private void handleDocumentUploaded(DocumentEvent event) throws Exception {
        log.info("Indicizzazione documento: {}", event.documentId);

        if (event.snippet == null || event.snippet.isBlank()) {
            log.warn("Snippet vuoto per documentId={}, indicizzazione saltata", event.documentId);
            return;
        }

        // 1. Genera embedding protetto da Retry
        Supplier<float[]> embeddingSupplier = Retry.decorateSupplier(
                ResilienceConfig.getEmbeddingRetry(),
                () -> embeddingProvider.createEmbedding(event.snippet)
        );
        float[] embedding = embeddingSupplier.get();

        // 2. Costruisce il punto vettoriale con payload
        PointStruct point = PointStruct.newBuilder()
                .setId(id(UUID.fromString(event.documentId)))
                .setVectors(vectors(embedding))
                .putAllPayload(Map.of(
                        "documentId", value(event.documentId),
                        "userId",     value(event.userId),
                        "filename",   value(event.filename),
                        "snippet",    value(event.snippet)
                ))
                .build();

        // 3. Upsert in Qdrant protetto da Retry
        Runnable qdrantUpsert = Retry.decorateRunnable(
                ResilienceConfig.getQdrantRetry(),
                () -> {
                    try {
                        qdrantClient.upsertAsync(COLLECTION_NAME, List.of(point)).get();
                    } catch (Exception e) {
                        throw new RuntimeException("Errore Qdrant Upsert", e);
                    }
                }
        );
        qdrantUpsert.run();

        log.info("Documento '{}' indicizzato con successo in Qdrant", event.documentId);
    }

    // ─────────────────────────────────────────────
    // SS-BE-10: document.updated
    // ─────────────────────────────────────────────
    private void handleDocumentUpdated(DocumentEvent event) throws Exception {
        log.info("Re-indicizzazione documento aggiornato: {}", event.documentId);
        // L'operazione upsert è idempotente: sovrascrive il vettore precedente
        handleDocumentUploaded(event);
    }

    // ─────────────────────────────────────────────
    // SS-BE-11: document.deleted
    // ─────────────────────────────────────────────
    private void handleDocumentDeleted(DocumentEvent event) throws Exception {
        log.info("Rimozione documento da Qdrant: {}", event.documentId);

        // Cancellazione da Qdrant protetta da Retry
        Runnable qdrantDelete = Retry.decorateRunnable(
                ResilienceConfig.getQdrantRetry(),
                () -> {
                    try {
                        qdrantClient.deleteAsync(COLLECTION_NAME,
                                List.of(id(UUID.fromString(event.documentId)))).get();
                    } catch (Exception e) {
                        throw new RuntimeException("Errore Qdrant Delete", e);
                    }
                }
        );
        qdrantDelete.run();

        log.info("Documento '{}' rimosso da Qdrant", event.documentId);
    }

    // ─────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────

    // Interfaccia personalizzata per poter passare il Channel al blocco try/catch
    @FunctionalInterface
    private interface ChannelDeliverCallback {
        void handle(Channel channel, String consumerTag, Delivery delivery) throws IOException;
    }

    private void registerConsumer(Connection connection, String queueName,
                                  ChannelDeliverCallback callback) throws IOException {
        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, true, false, false, null);
        channel.basicQos(1);
        
       
        channel.basicConsume(queueName, false, (tag, delivery) -> {
            callback.handle(channel, tag, delivery);
        }, tag -> {});
        
        log.info("Consumer registrato sulla coda '{}'", queueName);
    }
}