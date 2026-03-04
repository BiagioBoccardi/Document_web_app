package com.example.document_service.exception;

public class PayloadTooLargeException extends RuntimeException {
    public PayloadTooLargeException(String message) {
        super(message);
    }

    public PayloadTooLargeException(String field, String value) {
        super("Payload too large for " + field + ": " + value);
    }

}
