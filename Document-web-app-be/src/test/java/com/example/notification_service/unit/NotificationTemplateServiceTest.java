package com.example.notification_service.unit;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.notification_service.service.NotificationTemplateService;

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

    @Test
    void testUnknownTemplateKey() {
        String message = templateService.generateMessage("unknown.key", null);
        // Verifica il comportamento attuale del tuo codice
        assertTrue(message.contains("notifica"), "Dovrebbe restituire un testo di fallback");
    }

    @Test
    void testTemplateWithMissingMetadata() {
        String message = templateService.generateMessage("document.uploaded", Map.of());
        assertTrue(message.contains("N/A") || message.contains("documento"), "Dovrebbe gestire metadati mancanti");
    }
}
