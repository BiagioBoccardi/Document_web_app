package com.example.searchservice.embedding;

import java.util.List;

public interface EmbeddingProvider {
    List<Float> createEmbedding(String text);
}
