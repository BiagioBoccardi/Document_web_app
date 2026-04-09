package com.example.search_service.integration;

import com.google.common.util.concurrent.Futures;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.search_service.auth.JwtAuthMiddleware;
import com.example.search_service.config.DotenvConfig;
import com.example.search_service.controller.SearchController;
import com.example.search_service.dto.ErrorResponse;
import com.example.search_service.embedding.EmbeddingProvider;
import com.google.common.util.concurrent.ListenableFuture;
import io.javalin.Javalin;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.RetrievedPoint;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import io.qdrant.client.grpc.Points.WithVectorsSelector;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("SearchController - Test di integrazione")
class SearchControllerTest {

    private static final String SECRET  = DotenvConfig.get("JWT_SECRET", "default-secret");
    private static final String USER_ID = "test-user-" + UUID.randomUUID();
    private static final String DOC_ID  = UUID.randomUUID().toString();

    // Mock delle dipendenze — creati una volta sola, resettati prima di ogni test
    private final EmbeddingProvider mockProvider = mock(EmbeddingProvider.class);
    private final QdrantClient      mockQdrant   = mock(QdrantClient.class);

    private Javalin    app;
    private int        port;
    private HttpClient httpClient;
    private String     validToken;

    // ─────────────────────────────────────────────
    // Setup / Teardown
    // ─────────────────────────────────────────────
    @BeforeAll
    void startServer() {
        SearchController controller = new SearchController(mockProvider, mockQdrant);

        app = Javalin.create(config -> {
            config.http.defaultContentType = "application/json";
            config.showJavalinBanner = false;
        }).start(0); // porta 0 = OS assegna una porta libera

        app.exception(Exception.class, (e, ctx) ->
                ctx.status(500).json(new ErrorResponse(500, "Errore interno del server")));
        app.before("/api/v1/*", JwtAuthMiddleware::handle);
        controller.registerRoutes(app);
        app.get("/health", ctx -> ctx.json("{\"status\": \"UP\"}"));

        port = app.port();
        httpClient = HttpClient.newHttpClient();
        validToken = JWT.create()
                .withSubject(USER_ID)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3_600_000))
                .sign(Algorithm.HMAC256(SECRET));
    }

    @BeforeEach
    void resetMocks() {
        reset(mockProvider, mockQdrant);
        lenient().when(mockProvider.createEmbedding(anyString())).thenReturn(new float[384]);
        
        // Modifica: Futures.immediateFuture invece di CompletableFuture
        lenient().when(mockQdrant.searchAsync(any(SearchPoints.class)))
                .thenReturn(Futures.immediateFuture(List.of()));
        
        // Modifica: Futures.immediateFuture invece di CompletableFuture
        lenient().when(mockQdrant.retrieveAsync(anyString(), anyList(),
                any(WithPayloadSelector.class), any(WithVectorsSelector.class), any()))
                .thenReturn(Futures.immediateFuture(List.of()));
    }

    @AfterAll
    void stopServer() {
        if (app != null) app.stop();
    }

    // ─────────────────────────────────────────────
    // Helper: costruisce una richiesta HTTP
    // ─────────────────────────────────────────────
    private HttpResponse<String> get(String path) throws Exception {
        return get(path, null);
    }

    private HttpResponse<String> get(String path, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET();
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    // ═════════════════════════════════════════════
    // GET /health
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("GET /health")
    class HealthCheck {

        @Test
        @DisplayName("OK: risponde 200 senza token")
        void shouldReturn200WithoutToken() throws Exception {
            HttpResponse<String> res = get("/health");
            assertEquals(200, res.statusCode());
        }
    }

    // ═════════════════════════════════════════════
    // POST /api/v1/embeddings
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/v1/embeddings")
    class Embeddings {

        @Test
        @DisplayName("KO: nessun token — risponde 401")
        void shouldReturn401WithoutToken() throws Exception {
            HttpResponse<String> res = post("/api/v1/embeddings",
                    "{\"text\": \"testo\"}", null);
            assertEquals(401, res.statusCode());
        }

        @Test
        @DisplayName("OK: token valido e testo presente — risponde 200")
        void shouldReturn200WithValidRequest() throws Exception {
            HttpResponse<String> res = post("/api/v1/embeddings",
                    "{\"text\": \"testo di prova\"}", validToken);
            assertEquals(200, res.statusCode());
        }

        @Test
        @DisplayName("KO: testo vuoto — risponde 400")
        void shouldReturn400WhenTextIsEmpty() throws Exception {
            HttpResponse<String> res = post("/api/v1/embeddings",
                    "{\"text\": \"\"}", validToken);
            assertEquals(400, res.statusCode());
        }
    }

    // ═════════════════════════════════════════════
    // POST /api/v1/search
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/v1/search")
    class Search {

        @Test
        @DisplayName("KO: nessun token — risponde 401")
        void shouldReturn401WithoutToken() throws Exception {
            HttpResponse<String> res = post("/api/v1/search",
                    "{\"query\": \"contratto\", \"topK\": 5}", null);
            assertEquals(401, res.statusCode());
        }

        @Test
        @DisplayName("OK: token valido e body corretto — risponde 200")
        void shouldReturn200WithValidRequest() throws Exception {
            HttpResponse<String> res = post("/api/v1/search",
                    "{\"query\": \"contratto di lavoro\", \"topK\": 5}", validToken);
            assertEquals(200, res.statusCode());
        }

        @Test
        @DisplayName("KO: query vuota — risponde 400")
        void shouldReturn400WhenQueryIsEmpty() throws Exception {
            HttpResponse<String> res = post("/api/v1/search",
                    "{\"query\": \"\", \"topK\": 5}", validToken);
            assertEquals(400, res.statusCode());
        }

        @Test
        @DisplayName("KO: topK fuori range — risponde 422")
        void shouldReturn422WhenTopKIsOutOfRange() throws Exception {
            HttpResponse<String> res = post("/api/v1/search",
                    "{\"query\": \"test\", \"topK\": 999}", validToken);
            assertEquals(422, res.statusCode());
        }

        @Test
        @DisplayName("KO: topK negativo — risponde 422")
        void shouldReturn422WhenTopKIsNegative() throws Exception {
            HttpResponse<String> res = post("/api/v1/search",
                    "{\"query\": \"test\", \"topK\": -1}", validToken);
            assertEquals(422, res.statusCode());
        }
    }

    // ═════════════════════════════════════════════
    // GET /api/v1/search/similar/{documentId}
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/v1/search/similar/{documentId}")
    class Similar {

        @Test
        @DisplayName("KO: nessun token — risponde 401")
        void shouldReturn401WithoutToken() throws Exception {
            HttpResponse<String> res = get("/api/v1/search/similar/" + DOC_ID);
            assertEquals(401, res.statusCode());
        }

        @Test
        @DisplayName("KO: documentId non è un UUID valido — risponde 400")
        void shouldReturn400WhenDocumentIdIsNotUuid() throws Exception {
            HttpResponse<String> res = get("/api/v1/search/similar/non-un-uuid", validToken);
            assertEquals(400, res.statusCode());
        }

        @Test
        @DisplayName("KO: documento non trovato in Qdrant — risponde 404")
        void shouldReturn404WhenDocumentNotFound() throws Exception {
            // Modifica: Futures.immediateFuture
            when(mockQdrant.retrieveAsync(anyString(), anyList(),
                    any(WithPayloadSelector.class), any(WithVectorsSelector.class), any()))
                    .thenReturn(Futures.immediateFuture(List.of()));

            HttpResponse<String> res = get("/api/v1/search/similar/" + DOC_ID, validToken);
            assertEquals(404, res.statusCode());
        }

        @Test
        @DisplayName("OK: documento trovato — risponde 200")
        void shouldReturn200WhenDocumentFound() throws Exception {
            float[] vec = new float[384];
            io.qdrant.client.grpc.Points.Vectors vectors =
                    io.qdrant.client.grpc.Points.Vectors.newBuilder()
                            .setVector(io.qdrant.client.grpc.Points.Vector.newBuilder()
                                    .addAllData(floatArrayToList(vec))
                                    .build())
                            .build();

            RetrievedPoint point = RetrievedPoint.newBuilder()
                    .setVectors(vectors)
                    .build();

            // Modifica: Futures.immediateFuture
            when(mockQdrant.retrieveAsync(anyString(), anyList(),
                    any(WithPayloadSelector.class), any(WithVectorsSelector.class), any()))
                    .thenReturn(Futures.immediateFuture(List.of(point)));
            
            // Modifica: Futures.immediateFuture
            when(mockQdrant.searchAsync(any(SearchPoints.class)))
                    .thenReturn(Futures.immediateFuture(List.of()));

            HttpResponse<String> res = get("/api/v1/search/similar/" + DOC_ID, validToken);
            assertEquals(200, res.statusCode());
        }

        @Test
        @DisplayName("KO: topK non numerico — risponde 400")
        void shouldReturn400WhenTopKIsNotNumeric() throws Exception {
            HttpResponse<String> res = get(
                    "/api/v1/search/similar/" + DOC_ID + "?topK=abc", validToken);
            assertEquals(400, res.statusCode());
        }
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────
    private List<Float> floatArrayToList(float[] arr) {
        List<Float> list = new java.util.ArrayList<>();
        for (float f : arr) list.add(f);
        return list;
    }
}