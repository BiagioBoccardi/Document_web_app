package com.example.search_service.embedding;

import java.util.List;

public interface EmbeddingProvider {
    List<Float> createEmbedding(String text);
}
