package com.example.search_service.dto;

/**
 * DTO per la richiesta di generazione embedding.
 * POST /api/v1/embeddings
 */
public class EmbeddingRequest {
    public String text;
}