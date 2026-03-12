package com.example.search_service.dto;

/**
 * DTO per la risposta della generazione embedding.
 * POST /api/v1/embeddings
 */
public class EmbeddingResponse {

    public float[] embedding;
    public int dimensions;
    public String model;

    public EmbeddingResponse(float[] embedding, String model) {
        this.embedding = embedding;
        this.dimensions = embedding.length;
        this.model = model;
    }
}
