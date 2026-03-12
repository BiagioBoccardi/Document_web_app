package com.example.search_service.embedding;

import java.util.Random;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementazione Mock per sviluppo e fallback.
 * Genera vettori casuali senza fare chiamate di rete.
 */
@Slf4j
public class MockEmbeddingProvider implements EmbeddingProvider {

    private final int vectorSize;
    private final Random random;

    public MockEmbeddingProvider() {
        // 384 è la dimensione scelta in SearchService per Qdrant
        this.vectorSize = 384;
        this.random = new Random();
        log.warn("MockEmbeddingProvider attivo — nessun consumo API, vettori casuali");
    }

    @Override
    public float[] createEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Il testo per l'embedding non può essere nullo o vuoto");
        }

        float[] embedding = new float[vectorSize];
        for (int i = 0; i < vectorSize; i++) {
            // Valori casuali tra -1.0 e 1.0
            embedding[i] = (random.nextFloat() * 2) - 1.0f;
        }

        log.debug("MockEmbeddingProvider: generato vettore casuale di {} dimensioni", vectorSize);
        return embedding;
    }
}