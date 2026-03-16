package com.example.notification_service.messaging;

import java.util.Map;

import lombok.Data;

@Data
public class GenericEvent {
    private String type; // es: "user.registered", "document.uploaded"
    private int userId;
    private Map<String, Object> metadata; // Dati extra (es: nome file, query di ricerca)
}
