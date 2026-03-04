package com.example.document_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import com.example.document_service.event.DocumentEvent;
import com.example.document_service.event.DocumentEventPublisher;
import com.example.document_service.model.Document;
import com.example.document_service.model.DocumentMetadata;
import com.example.document_service.repository.DocumentRepository;

public class DocumentService {

	private static final String DEFAULT_MIME_TYPE = "text/plain";
	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final DocumentRepository documentRepository;
	private final DocumentEventPublisher eventPublisher;

	public DocumentService(DocumentRepository documentRepository, DocumentEventPublisher eventPublisher) {
		this.documentRepository = documentRepository;
		this.eventPublisher = eventPublisher;
	}

	public Document uploadDocument(long userId, String filename, String content, String mimeType) {
		validateUserId(userId);
		validateFilename(filename);
		validateContent(content);

		Date now = new Date();
		String normalizedMimeType = normalizeMimeType(mimeType);

		Document document = new Document();
		document.setUserId(userId);
		document.setFilename(filename.trim());
		document.setContent(content);
		document.setUploadDate(now);
		document.setLastModified(now);
		document.setMetadata(buildMetadata(content, normalizedMimeType));

		Document created = documentRepository.create(document);
		publishEvent("document.uploaded", created);
		return created;
	}

	public List<Document> getUserDocuments(long userId) {
		return getUserDocuments(userId, DEFAULT_PAGE, DEFAULT_SIZE, "desc");
	}

	public List<Document> getUserDocuments(long userId, int page, int size, String sort) {
		validateUserId(userId);
		validatePage(page);
		int normalizedSize = normalizeSize(size);
		boolean sortAscending = parseSortAscending(sort);
		return documentRepository.findByUserId(userId, page, normalizedSize, sortAscending);
	}

	public Document getDocumentById(long userId, String documentId) {
		validateUserId(userId);
		validateDocumentId(documentId);

		return documentRepository.findByIdAndUserId(documentId, userId)
				.orElseThrow(() -> new NoSuchElementException("Documento non trovato"));
	}

	public Document updateDocument(long userId, String documentId, String filename, String content, String mimeType) {
		validateUserId(userId);
		validateDocumentId(documentId);
		validateFilename(filename);
		validateContent(content);

		Document existing = documentRepository.findByIdAndUserId(documentId, userId)
				.orElseThrow(() -> new NoSuchElementException("Documento non trovato"));

		String normalizedMimeType = normalizeMimeType(mimeType);
		existing.setFilename(filename.trim());
		existing.setContent(content);
		existing.setLastModified(new Date());
		existing.setMetadata(buildMetadata(content, normalizedMimeType));

		Document updated = documentRepository.update(existing)
				.orElseThrow(() -> new NoSuchElementException("Documento non trovato"));

		publishEvent("document.updated", updated);

		return updated;
	}

	public boolean deleteDocument(long userId, String documentId) {
		validateUserId(userId);
		validateDocumentId(documentId);

		Document existing = documentRepository.findByIdAndUserId(documentId, userId).orElse(null);
		boolean deleted = documentRepository.deleteByIdAndUserId(documentId, userId);

		if (deleted && existing != null) {
			publishEvent("document.deleted", existing);
		}

		return deleted;
	}

	public long deleteAllDocumentsByUserId(long userId) {
		validateUserId(userId);
		return documentRepository.deleteAllByUserId(userId);
	}

	private void validateUserId(long userId) {
		if (userId <= 0) {
			throw new IllegalArgumentException("userId non valido");
		}
	}

	private void validateFilename(String filename) {
		if (filename == null || filename.isBlank()) {
			throw new IllegalArgumentException("filename obbligatorio");
		}

		if (!filename.toLowerCase().endsWith(".txt")) {
			throw new IllegalArgumentException("Sono supportati solo documenti .txt");
		}
	}

	private void validateContent(String content) {
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("content obbligatorio");
		}
	}

	private void validateDocumentId(String documentId) {
		if (documentId == null || documentId.isBlank()) {
			throw new IllegalArgumentException("documentId obbligatorio");
		}
	}

	private void validatePage(int page) {
		if (page < 0) {
			throw new IllegalArgumentException("page deve essere >= 0");
		}
	}

	private int normalizeSize(int size) {
		if (size <= 0) {
			return DEFAULT_SIZE;
		}
		if (size > MAX_SIZE) {
			return MAX_SIZE;
		}
		return size;
	}

	private boolean parseSortAscending(String sort) {
		if (sort == null || sort.isBlank()) {
			return false;
		}

		String value = sort.toLowerCase();
		if (!value.equals("asc") && !value.equals("desc")) {
			throw new IllegalArgumentException("sort deve essere 'asc' o 'desc'");
		}

		return value.equals("asc");
	}

	private String normalizeMimeType(String mimeType) {
		if (mimeType == null || mimeType.isBlank()) {
			return DEFAULT_MIME_TYPE;
		}
		return mimeType;
	}

	private void publishEvent(String eventName, Document document) {
		eventPublisher.publish(new DocumentEvent(
				eventName,
				document.getId(),
				document.getUserId(),
				document.getFilename(),
				Instant.now()));
	}

	private DocumentMetadata buildMetadata(String content, String mimeType) {
		byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
		long size = contentBytes.length;
		String checksum = sha256Hex(contentBytes);
		return new DocumentMetadata(size, mimeType, checksum);
	}

	private String sha256Hex(byte[] contentBytes) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(contentBytes);
			StringBuilder builder = new StringBuilder();
			for (byte item : hash) {
				builder.append(String.format("%02x", item));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("Algoritmo SHA-256 non disponibile", exception);
		}
	}
}
