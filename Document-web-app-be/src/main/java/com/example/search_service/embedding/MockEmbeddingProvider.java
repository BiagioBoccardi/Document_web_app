package com.example.search_service.embedding;

import java.util.Random;

/**
 * Implementazione Mock per sviluppo e fallback.
 * Genera vettori casuali senza fare chiamate di rete.
 */
public class MockEmbeddingProvider implements EmbeddingProvider {

    private final int vectorSize;
    private final Random random;

    public MockEmbeddingProvider() {
        // 384 è la dimensione scelta in SearchService per Qdrant
        this.vectorSize = 384; 
        this.random = new Random();
        System.out.println("--- [WARN] Inizializzato MockEmbeddingProvider (Nessun consumo API) ---");
    }

    @Override
    public float[] createEmbedding(String text) {
        float[] embedding = new float[vectorSize];
        
        // Genera un vettore casuale
        for (int i = 0; i < vectorSize; i++) {
            // Valori casuali tra -1.0 e 1.0
            embedding[i] = (random.nextFloat() * 2) - 1.0f;
        }
        
        return embedding;
    }
}