package com.example.document_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.document_service.service.DocumentService;

public class UserDeletedEventConsumer {
    private static final Logger logger = LoggerFactory.getLogger(UserDeletedEventConsumer.class);

    private final DocumentService documentService;

    public UserDeletedEventConsumer(DocumentService documentService) {
        this.documentService = documentService;
    }

    public long consume(UserDeletedEvent event) {
        long deletedCount = documentService.deleteAllDocumentsByUserId(event.getUserId());
        logger.info("Consumed user.deleted userId={} deletedDocuments={} eventTs={}",
                event.getUserId(),
                deletedCount,
                event.getTimestamp());
        return deletedCount;
    }
}
