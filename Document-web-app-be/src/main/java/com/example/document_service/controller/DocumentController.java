package com.example.document_service.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.example.document_service.exception.PayloadTooLargeException;
import com.example.document_service.exception.UnauthorizedException;
import com.example.document_service.exception.UnsupportedMediaTypeException;
import com.example.document_service.http.DocumentAuthMiddleware;
import com.example.document_service.model.Document;
import com.example.document_service.service.DocumentService;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

public class DocumentController {
	private static final String UPLOAD_FIELD = "file";
	private static final long MAX_FILE_SIZE_BYTES = 1_000_000;

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	public void registerRoutes(Javalin app) {
		app.post("/api/v1/documents", this::uploadDocument);
		app.get("/api/v1/documents", this::getUserDocuments);
		app.get("/api/v1/documents/{id}", this::getDocumentById);
		app.put("/api/v1/documents/{id}", this::updateDocument);
		app.delete("/api/v1/documents/{id}", this::deleteDocument);
	}

	private void uploadDocument(Context context) {
		long userId = extractUserId(context);
		UploadedFile uploadedFile = context.uploadedFile(UPLOAD_FIELD);
		if (uploadedFile == null) {
			throw new IllegalArgumentException("Campo multipart 'file' obbligatorio");
		}

		String filename = uploadedFile.filename();
		if (filename.isBlank() || !filename.toLowerCase().endsWith(".txt")) {
			throw new UnsupportedMediaTypeException("Sono supportati solo file .txt");
		}

		if (uploadedFile.size() > MAX_FILE_SIZE_BYTES) {
			throw new PayloadTooLargeException("File troppo grande: massimo 1MB");
		}

		String content = extractTextContent(uploadedFile);

		Document document = documentService.uploadDocument(
				userId,
				filename,
				content,
				uploadedFile.contentType());

		context.status(201).json(document);
	}

	private void getUserDocuments(Context context) {
		long userId = extractUserId(context);
		int page = getQueryInt(context, "page", 0);
		int size = getQueryInt(context, "size", 20);
		String sort = context.queryParam("sort");
		List<Document> documents = documentService.getUserDocuments(userId, page, size, sort);
		context.status(200).json(documents);
	}

	private void getDocumentById(Context context) {
		long userId = extractUserId(context);
		String documentId = context.pathParam("id");

		Document document = documentService.getDocumentById(userId, documentId);
		context.status(200).json(document);
	}

	private void updateDocument(Context context) {
		long userId = extractUserId(context);
		String documentId = context.pathParam("id");
		DocumentPayload payload = context.bodyAsClass(DocumentPayload.class);

		Document document = documentService.updateDocument(
				userId,
				documentId,
				payload.getFilename(),
				payload.getContent(),
				payload.getMimeType());

		context.status(200).json(document);
	}

	private void deleteDocument(Context context) {
		long userId = extractUserId(context);
		String documentId = context.pathParam("id");

		documentService.deleteDocument(userId, documentId);
		context.status(204);
	}

	private long extractUserId(Context context) {
		Long userId = context.attribute(DocumentAuthMiddleware.USER_ID_CONTEXT_KEY);
		if (userId == null || userId <= 0) {
			throw new UnauthorizedException("Utente non autenticato");
		}
		return userId;
	}

	private int getQueryInt(Context context, String name, int fallback) {
		return context.queryParamAsClass(name, Integer.class).getOrDefault(fallback);
	}

	private String extractTextContent(UploadedFile uploadedFile) {
		try (var inputStream = uploadedFile.content()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new IllegalStateException("Errore durante la lettura del file", exception);
		}
	}

	public static class DocumentPayload {
		private String filename;
		private String content;
		private String mimeType;

		public String getFilename() {
			return filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public String getMimeType() {
			return mimeType;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}
	}
}
