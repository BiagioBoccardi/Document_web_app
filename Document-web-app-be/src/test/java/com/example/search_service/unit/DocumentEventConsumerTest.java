package com.example.search_service.unit;

import com.example.search_service.config.RabbitMQConfig;
import com.example.search_service.dto.DocumentEvent;
import com.example.search_service.embedding.EmbeddingProvider;
import com.example.search_service.messaging.DocumentEventConsumer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.UpdateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DocumentEventConsumer - Test unitari")
class DocumentEventConsumerTest {

    @Mock private EmbeddingProvider embeddingProvider;
    @Mock private QdrantClient qdrantClient;
    @Mock private Connection connection;
    @Mock private Channel channel;
    @Mock private ListenableFuture<UpdateResult> updateFuture;

    private DocumentEventConsumer consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cattura le DeliverCallback registrate per ciascuna coda
    private final Map<String, DeliverCallback> callbacks = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        consumer = new DocumentEventConsumer(embeddingProvider, qdrantClient);

        when(connection.createChannel()).thenReturn(channel);

        // Cattura la callback per ogni coda; specifica CancelCallback per risolvere l'ambiguità
        doAnswer(inv -> {
            callbacks.put(inv.getArgument(0), inv.getArgument(2));
            return "consumerTag";
        }).when(channel).basicConsume(anyString(), anyBoolean(),
                any(DeliverCallback.class), any(CancelCallback.class));

        // upsertAsync e deleteAsync restituiscono ListenableFuture (Guava)
        when(qdrantClient.upsertAsync(anyString(), anyList())).thenReturn(updateFuture);
        when(qdrantClient.deleteAsync(anyString(), anyList())).thenReturn(updateFuture);

        when(embeddingProvider.createEmbedding(anyString())).thenReturn(new float[384]);

        consumer.startConsuming(connection);
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────
    private DocumentEvent buildEvent(String snippet) {
        DocumentEvent e = new DocumentEvent();
        e.documentId = UUID.randomUUID().toString();
        e.userId     = "user-test-uuid";
        e.filename   = "documento.pdf";
        e.snippet    = snippet;
        return e;
    }

    private void dispatch(String queue, DocumentEvent event) throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(event);
        Delivery delivery = mock(Delivery.class);
        com.rabbitmq.client.Envelope envelope = mock(com.rabbitmq.client.Envelope.class);
        when(delivery.getBody()).thenReturn(body);
        when(delivery.getEnvelope()).thenReturn(envelope);
        when(envelope.getDeliveryTag()).thenReturn(1L);
        callbacks.get(queue).handle("tag", delivery);
    }

    // ═════════════════════════════════════════════
    // document.uploaded
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("document.uploaded")
    class DocumentUploaded {

        @Test
        @DisplayName("OK: evento valido — genera embedding e fa upsert in Qdrant")
        void shouldIndexDocumentOnUpload() throws Exception {
            DocumentEvent event = buildEvent("snippet del documento caricato");

            dispatch(RabbitMQConfig.QUEUE_DOCUMENT_UPLOADED, event);

            verify(embeddingProvider).createEmbedding(eq("snippet del documento caricato"));
            verify(qdrantClient).upsertAsync(eq("documents"), anyList());
        }

        @Test
        @DisplayName("KO: snippet vuoto — skip silenzioso, nessun upsert")
        void shouldSkipIndexingWhenSnippetIsEmpty() throws Exception {
            DocumentEvent event = buildEvent("   ");

            dispatch(RabbitMQConfig.QUEUE_DOCUMENT_UPLOADED, event);

            verify(embeddingProvider, never()).createEmbedding(anyString());
            verify(qdrantClient, never()).upsertAsync(anyString(), anyList());
        }

        @Test
        @DisplayName("KO: snippet null — skip silenzioso, nessun upsert")
        void shouldSkipIndexingWhenSnippetIsNull() throws Exception {
            DocumentEvent event = buildEvent(null);

            dispatch(RabbitMQConfig.QUEUE_DOCUMENT_UPLOADED, event);

            verify(embeddingProvider, never()).createEmbedding(anyString());
            verify(qdrantClient, never()).upsertAsync(anyString(), anyList());
        }
    }

    // ═════════════════════════════════════════════
    // document.updated
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("document.updated")
    class DocumentUpdated {

        @Test
        @DisplayName("OK: evento valido — rigenera embedding e fa upsert (sovrascrive)")
        void shouldReindexDocumentOnUpdate() throws Exception {
            DocumentEvent event = buildEvent("nuovo snippet dopo aggiornamento");

            dispatch(RabbitMQConfig.QUEUE_DOCUMENT_UPDATED, event);

            verify(embeddingProvider).createEmbedding(eq("nuovo snippet dopo aggiornamento"));
            verify(qdrantClient).upsertAsync(eq("documents"), anyList());
        }
    }

    // ═════════════════════════════════════════════
    // document.deleted
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("document.deleted")
    class DocumentDeleted {

        @Test
        @DisplayName("OK: evento valido — rimuove il vettore da Qdrant")
        void shouldDeleteVectorOnDocumentDeleted() throws Exception {
            DocumentEvent event = buildEvent(null);

            dispatch(RabbitMQConfig.QUEUE_DOCUMENT_DELETED, event);

            verify(qdrantClient).deleteAsync(eq("documents"), anyList());
            verify(qdrantClient, never()).upsertAsync(anyString(), anyList());
        }
    }
}
