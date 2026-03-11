package com.example.document_service.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

import org.bson.types.ObjectId;

import com.example.document_service.event.DocumentEvent;
import com.example.document_service.event.DocumentEventPublisher;
import com.example.document_service.model.Document;
import com.example.document_service.model.DocumentMetadata;
import com.example.document_service.repository.DocumentRepository;
import com.mongodb.client.gridfs.GridFSBucket;

import io.javalin.http.UploadedFile;
import net.sourceforge.tess4j.TesseractException;

public class DocumentService {

    private static final String DEFAULT_MIME_TYPE = "text/plain";
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final DocumentRepository documentRepository;
    private final DocumentEventPublisher eventPublisher;
    private final DocumentOCRService ocrService;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentEventPublisher eventPublisher,
                           DocumentOCRService ocrService) {
        this.documentRepository = documentRepository;
        this.eventPublisher = eventPublisher;
        this.ocrService = ocrService;
    }

    // --- Upload da testo ---
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
        document.setMetadata(buildMetadata(content, normalizedMimeType, "TXT"));

        Document created = documentRepository.create(document);
        publishEvent("document.uploaded", created);
        return created;
    }

    // --- Upload da UploadedFile (TXT / PDF / DOC / XLS) ---
    public Document uploadDocument(long userId, UploadedFile uploadedFile) throws IOException, TesseractException {
        validateUserId(userId);
        if (uploadedFile == null || uploadedFile.filename().isBlank()) {
            throw new IllegalArgumentException("File obbligatorio");
        }

        String filename = uploadedFile.filename().trim();
        String fileType = getFileType(filename);

        // Estrai testo
        String content;
        if ("TXT".equals(fileType)) {
            content = extractText(uploadedFile);
        } else {
            content = ocrService.performOCR(uploadedFile);
        }

        // Salva binario su GridFS
        GridFSBucket bucket = documentRepository.getGridFSBucket();
        ObjectId gridFSId;
        try (InputStream inputStream = uploadedFile.content()) {
            gridFSId = bucket.uploadFromStream(filename, inputStream);
        }

        // Crea Document
        Date now = new Date();
        Document document = new Document();
        document.setUserId(userId);
        document.setFilename(filename);
        document.setContent(content);
        document.setGridFSId(gridFSId);
        document.setUploadDate(now);
        document.setLastModified(now);
        document.setMetadata(buildMetadata(content, uploadedFile.contentType(), fileType));

        Document created = documentRepository.create(document);
        publishEvent("document.uploaded", created);
        return created;
    }

    // --- Altri metodi ---
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
        existing.setMetadata(buildMetadata(content, normalizedMimeType, getFileType(filename)));

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

    // --- Helper ---
    private void validateUserId(long userId) { if (userId <= 0) throw new IllegalArgumentException("userId non valido"); }
    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) throw new IllegalArgumentException("filename obbligatorio");
        String ext = getFileExtension(filename);
        if (!ext.equalsIgnoreCase("txt") &&
            !ext.equalsIgnoreCase("pdf") &&
            !ext.equalsIgnoreCase("doc") &&
            !ext.equalsIgnoreCase("docx") &&
            !ext.equalsIgnoreCase("xls") &&
            !ext.equalsIgnoreCase("xlsx")) {
            throw new IllegalArgumentException("Estensione file non supportata");
        }
    }
    private void validateContent(String content) { if (content == null || content.isBlank()) throw new IllegalArgumentException("content obbligatorio"); }
    private void validateDocumentId(String documentId) { if (documentId == null || documentId.isBlank()) throw new IllegalArgumentException("documentId obbligatorio"); }
    private void validatePage(int page) { if (page < 0) throw new IllegalArgumentException("page deve essere >= 0"); }
    private int normalizeSize(int size) { if (size <= 0) return DEFAULT_SIZE; if (size > MAX_SIZE) return MAX_SIZE; return size; }
    private boolean parseSortAscending(String sort) { if (sort == null || sort.isBlank()) return false; String v = sort.toLowerCase(); if (!v.equals("asc") && !v.equals("desc")) throw new IllegalArgumentException("sort deve essere 'asc' o 'desc'"); return v.equals("asc"); }
    private String normalizeMimeType(String mimeType) { return (mimeType == null || mimeType.isBlank()) ? DEFAULT_MIME_TYPE : mimeType; }
    private void publishEvent(String eventName, Document document) {
        eventPublisher.publish(new DocumentEvent(eventName, document.getId(), document.getUserId(), document.getFilename(), new Date().toInstant()));
    }

    private DocumentMetadata buildMetadata(String content, String mimeType, String fileType) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        long size = bytes.length;
        String checksum = sha256Hex(bytes);
        return new DocumentMetadata(size, mimeType, checksum, fileType);
    }

    private String sha256Hex(byte[] bytes) {
        try { MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] hash = digest.digest(bytes); StringBuilder sb = new StringBuilder(); for (byte b : hash) sb.append(String.format("%02x", b)); return sb.toString(); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("Algoritmo SHA-256 non disponibile", e); }
    }

    private String getFileExtension(String filename) {
        int idx = filename.lastIndexOf('.'); return (idx < 0) ? "" : filename.substring(idx + 1);
    }

    private String getFileType(String filename) { return getFileExtension(filename).toUpperCase(); }

    private String extractText(UploadedFile uploadedFile) throws IOException {
        try (InputStream is = uploadedFile.content()) { return new String(is.readAllBytes(), StandardCharsets.UTF_8); }
    }
}