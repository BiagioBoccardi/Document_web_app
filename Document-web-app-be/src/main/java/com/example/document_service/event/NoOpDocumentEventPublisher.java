package com.example.document_service.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpDocumentEventPublisher implements DocumentEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(NoOpDocumentEventPublisher.class);

    @Override
    public void publish(DocumentEvent event) {
        logger.info("[NO-OP EVENT] {} docId={} userId={} filename={} ts={}",
                event.getEventName(),
                event.getDocumentId(),
                event.getUserId(),
                event.getFilename(),
                event.getTimestamp());
    }
}
