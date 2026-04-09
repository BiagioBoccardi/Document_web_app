package com.example.search_service.dto;

import java.time.Instant;

/**
 * DTO standardizzato per le risposte di errore API.
 * SS-BE-13: Standardizzazione error handling
 */
public class ErrorResponse {

    public int status;
    public String error;
    public String timestamp;

    public ErrorResponse(int status, String error) {
        this.status = status;
        this.error = error;
        this.timestamp = Instant.now().toString();
    }
}
