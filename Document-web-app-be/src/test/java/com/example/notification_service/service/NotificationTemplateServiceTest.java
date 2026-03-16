package com.example.notification_service.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

public class NotificationTemplateServiceTest {
    private final NotificationTemplateService templateService = new NotificationTemplateService();

    @Test
    void testUserRegisteredTemplate() {
        String message = templateService.generateMessage("user.registered", null);
        assertTrue(message.contains("Benvenuto"), "Il messaggio di benvenuto non è corretto"); 
    }

    @Test
    void testDocumentUploadedTemplate() {
        Map<String, Object> metadata = Map.of("fileName", "tesi.pdf");
        String message = templateService.generateMessage("document.uploaded", metadata);
        assertTrue(message.contains("tesi.pdf"), "Il messaggio non include il nome del file"); 
    }
}
