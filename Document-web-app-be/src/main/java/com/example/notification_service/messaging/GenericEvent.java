package com.example.notification_service.messaging;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GenericEvent {
    private String type; // es: "user.registered", "document.uploaded"

    @JsonProperty("user_id")
    private int userId;
    
    private Map<String, Object> metadata; // Dati extra (es: nome file, query di ricerca)
}
