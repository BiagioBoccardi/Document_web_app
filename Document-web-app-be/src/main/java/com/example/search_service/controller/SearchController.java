package com.example.search_service.controller;

import com.example.search_service.dto.EmbeddingRequest;
import com.example.search_service.dto.EmbeddingResponse;
import com.example.search_service.embedding.EmbeddingProvider;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller per gli endpoint del Search Service.
 * SS-BE-05: POST /api/v1/embeddings
 */
@Slf4j
public class SearchController {

    private final EmbeddingProvider embeddingProvider;

    public SearchController(EmbeddingProvider embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/v1/embeddings", this::createEmbedding);
    }

    // POST /api/v1/embeddings
    // Protetto da JWT tramite middleware definito in SearchService.java
    private void createEmbedding(Context ctx) {
        EmbeddingRequest body = ctx.bodyAsClass(EmbeddingRequest.class);

        if (body.text == null || body.text.isBlank()) {
            log.warn("Richiesta embedding con testo nullo o vuoto");
            ctx.status(HttpStatus.BAD_REQUEST)
               .json("{\"error\": \"Il campo 'text' è obbligatorio e non può essere vuoto\"}");
            return;
        }

        try {
            log.info("Generazione embedding per testo di {} caratteri", body.text.length());
            float[] embedding = embeddingProvider.createEmbedding(body.text);
            String model = embeddingProvider.getClass().getSimpleName();

            ctx.status(HttpStatus.OK).json(new EmbeddingResponse(embedding, model));
            log.info("Embedding generato con successo: {} dimensioni", embedding.length);

        } catch (UnsupportedOperationException ex) {
            log.error("Provider di embedding non disponibile: {}", ex.getMessage());
            ctx.status(HttpStatus.SERVICE_UNAVAILABLE)
               .json("{\"error\": \"Provider di embedding non disponibile\"}");

        } catch (Exception ex) {
            log.error("Errore durante la generazione dell'embedding: {}", ex.getMessage());
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json("{\"error\": \"Errore interno durante la generazione dell'embedding\"}");
        }
    }
}