package com.example.notification_service.messaging;

import com.example.notification_service.service.NotificationService;

public class EventConsumer {
    private final NotificationService notificationService;

    public EventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void startListening() {
        // Stub implementation for RabbitMQ event consumption
        // In a real implementation, this would connect to RabbitMQ and listen for events
    }

    public void stopListening() {
        // Stub implementation for stopping RabbitMQ event consumption
    }
}