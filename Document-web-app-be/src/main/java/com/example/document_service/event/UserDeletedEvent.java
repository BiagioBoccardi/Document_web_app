package com.example.document_service.event;

import java.time.Instant;

public class UserDeletedEvent {
    private final long userId;
    private final Instant timestamp;

    public UserDeletedEvent(long userId, Instant timestamp) {
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public long getUserId() {
        return userId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
