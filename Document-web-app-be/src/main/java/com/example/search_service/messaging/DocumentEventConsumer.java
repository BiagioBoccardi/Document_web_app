package com.example.search_service.messaging;

import com.example.search_service.config.RabbitMQConfig;
import com.example.search_service.dto.DocumentEvent;
import com.example.search_service.embedding.EmbeddingProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointStruct;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

/**
 * Consumer RabbitMQ per gli eventi documento.
 * SS-BE-09: document.uploaded  → embedding + upsert Qdrant
 * SS-BE-10: document.updated   → re-embedding + upsert Qdrant
 * SS-BE-11: document.deleted   → delete da Qdrant
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
        registerConsumer(connection, RabbitMQConfig.QUEUE_DOCUMENT_UPLOADED, (tag, delivery) -> {
            String body = new String(delivery.getBody());
            log.info("Evento ricevuto su '{}': {}", RabbitMQConfig.QUEUE_DOCUMENT_UPLOADED, body);
            try {
                DocumentEvent event = objectMapper.readValue(body, DocumentEvent.class);
                handleDocumentUploaded(event);
            } catch (Exception e) {
                log.error("Errore nel processare document.uploaded: {}", e.getMessage());
            }
        });

        // SS-BE-10: document.updated
        registerConsumer(connection, RabbitMQConfig.QUEUE_DOCUMENT_UPDATED, (tag, delivery) -> {
            String body = new String(delivery.getBody());
            log.info("Evento ricevuto su '{}': {}", RabbitMQConfig.QUEUE_DOCUMENT_UPDATED, body);
            try {
                DocumentEvent event = objectMapper.readValue(body, DocumentEvent.class);
                handleDocumentUpdated(event);
            } catch (Exception e) {
                log.error("Errore nel processare document.updated: {}", e.getMessage());
            }
        });

        // SS-BE-11: document.deleted
        registerConsumer(connection, RabbitMQConfig.QUEUE_DOCUMENT_DELETED, (tag, delivery) -> {
            String body = new String(delivery.getBody());
            log.info("Evento ricevuto su '{}': {}", RabbitMQConfig.QUEUE_DOCUMENT_DELETED, body);
            try {
                DocumentEvent event = objectMapper.readValue(body, DocumentEvent.class);
                handleDocumentDeleted(event);
            } catch (Exception e) {
                log.error("Errore nel processare document.deleted: {}", e.getMessage());
            }
        });

        log.info("Consumer RabbitMQ avviati per le code: uploaded, updated, deleted");
    }

    // ─────────────────────────────────────────────
    // SS-BE-09: document.uploaded
    // ─────────────────────────────────────────────
    private void handleDocumentUploaded(DocumentEvent event) {
        log.info("Indicizzazione documento: {}", event.documentId);

        if (event.snippet == null || event.snippet.isBlank()) {
            log.warn("Snippet vuoto per documentId={}, indicizzazione saltata", event.documentId);
            return;
        }

        try {
            // 1. Genera embedding dallo snippet
            float[] embedding = embeddingProvider.createEmbedding(event.snippet);

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

            // 3. Upsert in Qdrant
            qdrantClient.upsertAsync(COLLECTION_NAME,
                    List.of(point)).get();

            log.info("Documento '{}' indicizzato con successo in Qdrant", event.documentId);

        } catch (Exception e) {
            log.error("Errore durante l'indicizzazione del documento '{}': {}",
                    event.documentId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // SS-BE-10: document.updated
    // ─────────────────────────────────────────────
    private void handleDocumentUpdated(DocumentEvent event) {
        log.info("Re-indicizzazione documento aggiornato: {}", event.documentId);
        // La logica è identica all'upload: upsert sovrascrive il vettore esistente
        handleDocumentUploaded(event);
    }

    // ─────────────────────────────────────────────
    // SS-BE-11: document.deleted
    // ─────────────────────────────────────────────
    private void handleDocumentDeleted(DocumentEvent event) {
        log.info("Rimozione documento da Qdrant: {}", event.documentId);

        try {
            qdrantClient.deleteAsync(COLLECTION_NAME,
                    List.of(id(UUID.fromString(event.documentId)))).get();

            log.info("Documento '{}' rimosso da Qdrant", event.documentId);

        } catch (Exception e) {
            log.error("Errore durante la rimozione del documento '{}': {}",
                    event.documentId, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // Helper: registra un consumer su una coda
    // ─────────────────────────────────────────────
    private void registerConsumer(Connection connection, String queueName,
                                   DeliverCallback callback) throws IOException {
        Channel channel = connection.createChannel();
        // Crea la coda se non esiste (idempotente)
        channel.queueDeclare(queueName, true, false, false, null);
        // Processa un messaggio alla volta
        channel.basicQos(1);
        channel.basicConsume(queueName, true, callback, tag -> {});
        log.info("Consumer registrato sulla coda '{}'", queueName);
    }
}