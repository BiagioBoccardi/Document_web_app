package com.example.document_service.controller;

import java.io.IOException;
import java.util.List;

import com.example.document_service.exception.PayloadTooLargeException;
import com.example.document_service.exception.UnauthorizedException;
import com.example.document_service.http.DocumentAuthMiddleware;
import com.example.document_service.model.Document;
import com.example.document_service.service.DocumentService;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import net.sourceforge.tess4j.TesseractException;

public class DocumentController {
    private static final String UPLOAD_FIELD = "file";
    private static final long MAX_FILE_SIZE_BYTES = 10_000_000;

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) { this.documentService = documentService; }

    public void registerRoutes(Javalin app) {
        registerRoutes(app, null);
    }
    public void registerRoutes(Javalin app, DocumentAuthMiddleware authMiddleware) {
        if (authMiddleware != null) {
            app.before("/api/v1/documents*", authMiddleware::authenticate);
        }
        app.post("/api/v1/documents", this::uploadDocument);
        app.get("/api/v1/documents", this::getUserDocuments);
        app.get("/api/v1/documents/{id}", this::getDocumentById);
        app.put("/api/v1/documents/{id}", this::updateDocument);
        app.delete("/api/v1/documents/{id}", this::deleteDocument);
    }

    private void uploadDocument(Context ctx) {
        long userId = extractUserId(ctx);
        UploadedFile uploadedFile = ctx.uploadedFile(UPLOAD_FIELD);
        if (uploadedFile == null ||uploadedFile.filename().isBlank()) {
            throw new IllegalArgumentException("Campo multipart 'file' obbligatorio");
        }
        if (uploadedFile.size() > MAX_FILE_SIZE_BYTES) {
            throw new PayloadTooLargeException("File troppo grande: massimo 10MB");
        }

        try {
            Document document = documentService.uploadDocument(userId, uploadedFile);
            ctx.status(201).json(document);
        } catch (IOException | TesseractException e) {
            throw new IllegalStateException("Errore durante l'elaborazione del file", e);
        }
    }

    private void getUserDocuments(Context ctx) {
        long userId = extractUserId(ctx);
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(0);
        int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(20);
        String sort = ctx.queryParam("sort");
        List<Document> documents = documentService.getUserDocuments(userId, page, size, sort);
        ctx.status(200).json(documents);
    }

    private void getDocumentById(Context ctx) {
        long userId = extractUserId(ctx);
        String documentId = ctx.pathParam("id");
        Document document = documentService.getDocumentById(userId, documentId);
        ctx.status(200).json(document);
    }

    private void updateDocument(Context ctx) {
        long userId = extractUserId(ctx);
        String documentId = ctx.pathParam("id");
        DocumentPayload payload = ctx.bodyAsClass(DocumentPayload.class);
        Document document = documentService.updateDocument(userId, documentId, payload.getFilename(), payload.getContent(), payload.getMimeType());
        ctx.status(200).json(document);
    }

    private void deleteDocument(Context ctx) {
        long userId = extractUserId(ctx);
        String documentId = ctx.pathParam("id");
        documentService.deleteDocument(userId, documentId);
        ctx.status(204);
    }

    private long extractUserId(Context ctx) {
        Long userId = ctx.attribute(DocumentAuthMiddleware.USER_ID_CONTEXT_KEY);
        if (userId == null || userId <= 0) throw new UnauthorizedException("Utente non autenticato");
        return userId;
    }

    public static class DocumentPayload {
        private String filename;
        private String content;
        private String mimeType;
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    }
}