package com.example.document_service.exception;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String field, String value) {
        super("Unauthorized access for " + field + ": " + value);
    }
}
