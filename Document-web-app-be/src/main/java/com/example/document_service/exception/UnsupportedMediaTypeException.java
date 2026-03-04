package com.example.document_service.exception;

public class UnsupportedMediaTypeException extends RuntimeException {
    public UnsupportedMediaTypeException(String message) {
        super(message);
    }

    public UnsupportedMediaTypeException(String field, String value) {
        super("Unsupported media type for " + field + ": " + value);
    }
}
