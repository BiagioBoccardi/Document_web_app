package com.example.document_service.controller;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.document_service.http.DocumentAuthMiddleware;
import com.example.document_service.http.DocumentHttpErrorHandler;
import com.example.document_service.model.Document;
import com.example.document_service.service.DocumentService;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.Request;
import okhttp3.Response;

public class DocumentControllerTest {

    private static final String JWT_SECRET = "test-secret-key-for-unit-tests";
    private static final String JWT_ISSUER = "";

    private DocumentService documentService;
    private DocumentController documentController;
    private DocumentAuthMiddleware authMiddleware;

    private Javalin buildTestApp() {
        documentController = new DocumentController(documentService);
        authMiddleware = new DocumentAuthMiddleware(JWT_SECRET, JWT_ISSUER);
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false);
        DocumentHttpErrorHandler.register(app);
        documentController.registerRoutes(app, authMiddleware);
        return app;
    }

    private Request buildGetRequest(int port, String path, String userId) {
        return new Request.Builder()
                .url("http://localhost:" + port + path)
                .header("X-User-Id", userId)
                .get()
                .build();
    }

    private Request buildGetRequest(int port, String path) {
        return new Request.Builder()
                .url("http://localhost:" + port + path)
                .get()
                .build();
    }

    private Request buildDeleteRequest(int port, String path, String userId) {
        return new Request.Builder()
                .url("http://localhost:" + port + path)
                .header("X-User-Id", userId)
                .delete()
                .build();
    }

    @Test
    void testGetUserDocuments() {
        documentService = mock(DocumentService.class);
        Document doc1 = new Document();
        doc1.setId("1");
        doc1.setUserId(1L);
        doc1.setFilename("file1.txt");

        when(documentService.getUserDocuments(eq(1L), anyInt(), anyInt(), any()))
                .thenReturn(List.of(doc1));

        JavalinTest.test(buildTestApp(), (server, client) -> {
            try (Response response = client.request(
                    buildGetRequest(server.port(), "/api/v1/documents", "1"))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).contains("file1.txt");
            }
        });

        verify(documentService).getUserDocuments(eq(1L), anyInt(), anyInt(), any());
    }

    @Test
    void testGetDocumentById() {
        documentService = mock(DocumentService.class);
        Document doc = new Document();
        doc.setId("123");
        doc.setUserId(1L);
        doc.setFilename("file1.txt");

        when(documentService.getDocumentById(1L, "123")).thenReturn(doc);

        JavalinTest.test(buildTestApp(), (server, client) -> {
            try (Response response = client.request(
                    buildGetRequest(server.port(), "/api/v1/documents/123", "1"))) {
                assertThat(response.code()).isEqualTo(200);
                assertThat(response.body().string()).contains("file1.txt");
            }
        });

        verify(documentService).getDocumentById(1L, "123");
    }

    @Test
    void testGetDocumentNotFound() {
        documentService = mock(DocumentService.class);
        when(documentService.getDocumentById(1L, "999"))
                .thenThrow(new NoSuchElementException("Documento non trovato"));

        JavalinTest.test(buildTestApp(), (server, client) -> {
            try (Response response = client.request(
                    buildGetRequest(server.port(), "/api/v1/documents/999", "1"))) {
                assertThat(response.code()).isEqualTo(404);
                assertThat(response.body().string()).contains("Documento non trovato");
            }
        });

        verify(documentService).getDocumentById(1L, "999");
    }

    @Test
    void testDeleteDocumentExisting() {
        documentService = mock(DocumentService.class);
        when(documentService.deleteDocument(1L, "123")).thenReturn(true);

        JavalinTest.test(buildTestApp(), (server, client) -> {
            try (Response response = client.request(
                    buildDeleteRequest(server.port(), "/api/v1/documents/123", "1"))) {
                assertThat(response.code()).isEqualTo(204);
            }
        });

        verify(documentService).deleteDocument(1L, "123");
    }

    @Test
    void testDeleteDocumentNotFound() {
        documentService = mock(DocumentService.class);
        when(documentService.deleteDocument(1L, "456")).thenReturn(false);

        JavalinTest.test(buildTestApp(), (server, client) -> {
            try (Response response = client.request(
                    buildDeleteRequest(server.port(), "/api/v1/documents/456", "1"))) {
                assertThat(response.code()).isEqualTo(204);
            }
        });

        verify(documentService).deleteDocument(1L, "456");
    }

    @Test
    void testMissingAuthReturns401() {
        documentService = mock(DocumentService.class);

        JavalinTest.test(buildTestApp(), (server, client) -> {
            try (Response response = client.request(
                    buildGetRequest(server.port(), "/api/v1/documents"))) {
                assertThat(response.code()).isEqualTo(401);
            }
        });
    }
}