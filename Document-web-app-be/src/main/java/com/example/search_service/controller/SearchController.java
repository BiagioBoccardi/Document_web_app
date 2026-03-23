package com.example.search_service.controller;

import com.example.search_service.dto.EmbeddingRequest;
import com.example.search_service.dto.EmbeddingResponse;
import com.example.search_service.dto.ErrorResponse;
import com.example.search_service.dto.SearchRequest;
import com.example.search_service.dto.SearchResponse;
import com.example.search_service.dto.SearchResult;
import com.example.search_service.embedding.EmbeddingProvider;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.RetrievedPoint;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import io.qdrant.client.grpc.Points.WithVectorsSelector;
import io.qdrant.client.grpc.JsonWithInt.Value;
import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.PointIdFactory.id;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller per gli endpoint del Search Service.
 * SS-BE-05: POST /api/v1/embeddings
 * SS-BE-06: POST /api/v1/search
 * SS-BE-08: GET  /api/v1/search/similar/{documentId}
 */
@Slf4j
public class SearchController {

    private static final String COLLECTION_NAME = "documents";
    private static final int MAX_TOP_K = 50;

    private final EmbeddingProvider embeddingProvider;
    private final QdrantClient qdrantClient;

    public SearchController(EmbeddingProvider embeddingProvider, QdrantClient qdrantClient) {
        this.embeddingProvider = embeddingProvider;
        this.qdrantClient = qdrantClient;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/v1/embeddings", this::createEmbedding);
        app.post("/api/v1/search", this::search);
        app.get("/api/v1/search/similar/{documentId}", this::findSimilar);
    }

    // -------------------------------------------------------------------------
    // SS-BE-05: POST /api/v1/embeddings
    // -------------------------------------------------------------------------
    private void createEmbedding(Context ctx) {
        EmbeddingRequest body = ctx.bodyAsClass(EmbeddingRequest.class);

        if (body.text == null || body.text.isBlank()) {
            log.warn("Richiesta embedding con testo nullo o vuoto");
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse(400, "Il campo 'text' è obbligatorio e non può essere vuoto"));
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
               .json(new ErrorResponse(503, "Provider di embedding non disponibile"));

        } catch (Exception ex) {
            log.error("Errore durante la generazione dell'embedding: {}", ex.getMessage());
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(new ErrorResponse(500, "Errore interno durante la generazione dell'embedding"));
        }
    }

    // -------------------------------------------------------------------------
    // SS-BE-06 + SS-BE-07: POST /api/v1/search
    // -------------------------------------------------------------------------
    private void search(Context ctx) {
        String userId = ctx.attribute("userId");
        if (userId == null) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(new ErrorResponse(401, "Token non valido o userId mancante"));
            return;
        }

        SearchRequest body = ctx.bodyAsClass(SearchRequest.class);

        if (body.query == null || body.query.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse(400, "Il campo 'query' è obbligatorio e non può essere vuoto"));
            return;
        }

        if (body.topK <= 0 || body.topK > MAX_TOP_K) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
               .json(new ErrorResponse(422, "Il campo 'topK' deve essere tra 1 e " + MAX_TOP_K));
            return;
        }

        try {
            log.info("Ricerca semantica per userId='{}', query='{}', topK={}",
                    userId, body.query, body.topK);
            float[] queryVector = embeddingProvider.createEmbedding(body.query);

            Filter userFilter = Filter.newBuilder()
                    .addMust(matchKeyword("userId", userId))
                    .build();

            List<Float> vectorList = new ArrayList<>();
            for (float v : queryVector) vectorList.add(v);

            SearchPoints searchPoints = SearchPoints.newBuilder()
                    .setCollectionName(COLLECTION_NAME)
                    .addAllVector(vectorList)
                    .setFilter(userFilter)
                    .setLimit(body.topK)
                    .setWithPayload(
                        io.qdrant.client.grpc.Points.WithPayloadSelector.newBuilder()
                            .setEnable(true)
                            .build())
                    .build();

            List<ScoredPoint> hits = qdrantClient.searchAsync(searchPoints).get();

            List<SearchResult> results = new ArrayList<>();
            for (ScoredPoint hit : hits) {
                Map<String, Value> payload = hit.getPayloadMap();
                results.add(new SearchResult(
                        getPayloadString(payload, "documentId"),
                        getPayloadString(payload, "filename"),
                        getPayloadString(payload, "snippet"),
                        hit.getScore()
                ));
            }

            log.info("Ricerca completata: {} risultati per userId='{}'", results.size(), userId);
            ctx.status(HttpStatus.OK).json(new SearchResponse(body.query, results));

        } catch (UnsupportedOperationException ex) {
            log.error("Provider di embedding non disponibile: {}", ex.getMessage());
            ctx.status(HttpStatus.SERVICE_UNAVAILABLE)
               .json(new ErrorResponse(503, "Provider di embedding non disponibile"));

        } catch (Exception ex) {
            log.error("Errore durante la ricerca semantica: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(new ErrorResponse(500, "Errore interno durante la ricerca"));
        }
    }

    // -------------------------------------------------------------------------
    // SS-BE-08: GET /api/v1/search/similar/{documentId}
    // -------------------------------------------------------------------------
    private void findSimilar(Context ctx) {
        String userId = ctx.attribute("userId");
        if (userId == null) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json(new ErrorResponse(401, "Token non valido o userId mancante"));
            return;
        }

        String documentId = ctx.pathParam("documentId");

        // Valida formato UUID del documentId prima di procedere
        UUID docUuid;
        try {
            docUuid = UUID.fromString(documentId);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST)
               .json(new ErrorResponse(400, "Il parametro 'documentId' non è un UUID valido"));
            return;
        }

        int topK = 5;
        String topKParam = ctx.queryParam("topK");
        if (topKParam != null) {
            try {
                topK = Integer.parseInt(topKParam);
                if (topK <= 0 || topK > MAX_TOP_K) {
                    ctx.status(HttpStatus.UNPROCESSABLE_CONTENT)
                       .json(new ErrorResponse(422, "Il parametro 'topK' deve essere tra 1 e " + MAX_TOP_K));
                    return;
                }
            } catch (NumberFormatException e) {
                ctx.status(HttpStatus.BAD_REQUEST)
                   .json(new ErrorResponse(400, "Il parametro 'topK' deve essere un numero intero"));
                return;
            }
        }

        try {
            // 1. Recupera il vettore del documento originale da Qdrant
            List<RetrievedPoint> retrieved = qdrantClient.retrieveAsync(
                    COLLECTION_NAME,
                    List.of(id(docUuid)),
                    WithPayloadSelector.newBuilder().setEnable(true).build(),
                    WithVectorsSelector.newBuilder().setEnable(true).build(),
                    null  // ReadConsistency — usa il default del server
            ).get();

            if (retrieved.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND)
                   .json(new ErrorResponse(404, "Documento con id '" + documentId + "' non trovato"));
                return;
            }

            List<Float> vector = retrieved.get(0).getVectors().getVector().getDataList();

            // 2. Filtro userId per isolamento
            Filter userFilter = Filter.newBuilder()
                    .addMust(matchKeyword("userId", userId))
                    .build();

            // 3. Cerca documenti simili (topK+1 per escludere il documento stesso)
            SearchPoints searchPoints = SearchPoints.newBuilder()
                    .setCollectionName(COLLECTION_NAME)
                    .addAllVector(vector)
                    .setFilter(userFilter)
                    .setLimit(topK + 1)
                    .setWithPayload(
                        io.qdrant.client.grpc.Points.WithPayloadSelector.newBuilder()
                            .setEnable(true)
                            .build())
                    .build();

            List<ScoredPoint> hits = qdrantClient.searchAsync(searchPoints).get();

            // 4. Mappa i risultati escludendo il documento originale
            List<SearchResult> results = new ArrayList<>();
            for (ScoredPoint hit : hits) {
                Map<String, Value> payload = hit.getPayloadMap();
                String hitDocId = getPayloadString(payload, "documentId");
                if (documentId.equals(hitDocId)) continue;
                if (results.size() >= topK) break;
                results.add(new SearchResult(
                        hitDocId,
                        getPayloadString(payload, "filename"),
                        getPayloadString(payload, "snippet"),
                        hit.getScore()
                ));
            }

            log.info("Similar search completata: {} risultati per documentId='{}', userId='{}'",
                    results.size(), documentId, userId);
            ctx.status(HttpStatus.OK).json(new SearchResponse(documentId, results));

        } catch (Exception ex) {
            log.error("Errore durante la ricerca documenti simili: {}", ex.getMessage(), ex);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .json(new ErrorResponse(500, "Errore interno durante la ricerca di documenti simili"));
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------
    private String getPayloadString(Map<String, Value> payload, String key) {
        Value v = payload.get(key);
        if (v == null) return null;
        return v.hasStringValue() ? v.getStringValue() : null;
    }
}
