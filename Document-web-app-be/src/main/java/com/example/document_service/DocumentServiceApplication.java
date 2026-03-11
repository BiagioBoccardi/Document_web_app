package com.example.document_service;

import java.util.Map;

import com.example.document_service.controller.DocumentController;
import com.example.document_service.event.DocumentEventPublisher;
import com.example.document_service.event.NoOpDocumentEventPublisher;
import com.example.document_service.http.DocumentAuthMiddleware;
import com.example.document_service.http.DocumentHttpErrorHandler;
import com.example.document_service.repository.DocumentRepository;
import com.example.document_service.service.DocumentOCRService;
import com.example.document_service.service.DocumentService;

import io.javalin.Javalin;

public class DocumentServiceApplication {
    private static final int DEFAULT_PORT = 8082;
    private static final String DEFAULT_MONGO_URI = "mongodb://localhost:27017";
    private static final String DEFAULT_MONGO_DB = "document_web_app";
    private static final String DEFAULT_MONGO_COLLECTION = "documents";
    private static final String DEFAULT_JWT_SECRET = "change-me-in-production";
    private static final String DEFAULT_JWT_ISSUER = "";

    private DocumentServiceApplication() {
    }

    public static void start() {
        int port = readPort();
        String mongoUri = readEnv("MONGO_URI", DEFAULT_MONGO_URI);
        String mongoDb = readEnv("MONGO_DB", DEFAULT_MONGO_DB);
        String mongoCollection = readEnv("MONGO_COLLECTION", DEFAULT_MONGO_COLLECTION);
        String jwtSecret = readEnv("JWT_SECRET", DEFAULT_JWT_SECRET);
        String jwtIssuer = readEnv("JWT_ISSUER", DEFAULT_JWT_ISSUER);

        DocumentRepository documentRepository = new DocumentRepository(mongoUri, mongoDb, mongoCollection);
        DocumentEventPublisher eventPublisher = new NoOpDocumentEventPublisher();
        DocumentOCRService ocrService = new DocumentOCRService();
        DocumentService documentService = new DocumentService(documentRepository, eventPublisher, ocrService);
        DocumentController documentController = new DocumentController(documentService);
        DocumentAuthMiddleware documentAuthMiddleware = new DocumentAuthMiddleware(jwtSecret, jwtIssuer);

        Javalin app = Javalin.create(config -> config.showJavalinBanner = false)
                .start(port);

        DocumentHttpErrorHandler.register(app);

        app.before("/api/v1/documents*", documentAuthMiddleware::authenticate);

        app.get("/health", context -> context.json(Map.of("status", "UP", "service", "document-service")));
        documentController.registerRoutes(app);

        app.events(eventConfig -> eventConfig.serverStopped(documentRepository::close));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                app.stop();
            } catch (Exception ignored) {
            }
        }));
    }

    private static int readPort() {
        String value = readEnv("APP_PORT", String.valueOf(DEFAULT_PORT));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return DEFAULT_PORT;
        }
    }

    private static String readEnv(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
