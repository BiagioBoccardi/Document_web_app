package com.example.document_service.event;

public interface DocumentEventPublisher {
    void publish(DocumentEvent event);
}
