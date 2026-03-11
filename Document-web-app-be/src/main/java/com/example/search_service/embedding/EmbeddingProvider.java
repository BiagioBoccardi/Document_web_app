package com.example.search_service.embedding;

import java.util.List;

/**
 * Interfaccia per la generazione di embedding (vettori).
 */
public interface EmbeddingProvider {
    float[] createEmbedding(String text);
}
