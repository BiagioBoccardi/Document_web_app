package com.example.document_service.event;

import java.time.Instant;

public class DocumentEvent {
    private final String eventName;
    private final String documentId;
    private final long userId;
    private final String filename;
    private final Instant timestamp;

    public DocumentEvent(String eventName, String documentId, long userId, String filename, Instant timestamp) {
        this.eventName = eventName;
        this.documentId = documentId;
        this.userId = userId;
        this.filename = filename;
        this.timestamp = timestamp;
    }

    public String getEventName() {
        return eventName;
    }

    public String getDocumentId() {
        return documentId;
    }

    public long getUserId() {
        return userId;
    }

    public String getFilename() {
        return filename;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
