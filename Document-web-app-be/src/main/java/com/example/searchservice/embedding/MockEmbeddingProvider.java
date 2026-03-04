package com.example.searchservice.embedding;

import java.util.Collections;
import java.util.List;

public class MockEmbeddingProvider implements EmbeddingProvider {
    private static final int MOCK_DIMENSION = 384; // Dimensione comune per embedding

    @Override
    public List<Float> createEmbedding(String text) {
        System.out.printf("--- MOCK: Creazione embedding per testo di lunghezza %d ...%n", text.length());
        return Collections.nCopies(MOCK_DIMENSION, 0.0f);
    }
}
