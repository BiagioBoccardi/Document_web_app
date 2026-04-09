package com.example.notification_service.service;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotificationTemplateService {
    /**
     * Genera il messaggio della notifica in base al tipo di evento e ai metadati.
     */
    public String generateMessage(String eventType, Map<String, Object> metadata) {
        switch (eventType) {
            case "user.registered":
                return "Benvenuto sulla piattaforma! Siamo felici di averti con noi.";

            case "document.uploaded":
                String fileName = metadata != null ? (String) metadata.getOrDefault("fileName", "documento") : "documento";
                return String.format("Il caricamento del documento '%s' è stato completato con successo.", fileName); 

            case "search.completed":
                String query = metadata != null ? (String) metadata.getOrDefault("query", "tua ricerca") : "tua ricerca";
                return String.format("La tua ricerca per '%s' è terminata. Clicca per vedere i risultati.", query); 

            default:
                log.warn("Tipo evento '{}' non mappato nei template. Uso messaggio di default.", eventType);
                return "Hai una nuova notifica dal sistema.";
        }
    }
}
