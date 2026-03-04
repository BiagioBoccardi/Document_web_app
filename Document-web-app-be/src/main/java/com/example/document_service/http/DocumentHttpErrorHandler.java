package com.example.document_service.http;

import java.util.NoSuchElementException;

import com.example.document_service.exception.PayloadTooLargeException;
import com.example.document_service.exception.UnauthorizedException;
import com.example.document_service.exception.UnsupportedMediaTypeException;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;

public class DocumentHttpErrorHandler {

    private DocumentHttpErrorHandler() {
    }

    public static void register(Javalin app) {
        app.exception(BadRequestResponse.class, (exception, context) -> {
            context.status(400).json(new ApiErrorResponse(400, "Bad Request", exception.getMessage(), context.path()));
        });

        app.exception(UnauthorizedException.class, (exception, context) -> {
            context.status(401).json(new ApiErrorResponse(401, "Unauthorized", exception.getMessage(), context.path()));
        });

        app.exception(SecurityException.class, (exception, context) -> {
            context.status(403).json(new ApiErrorResponse(403, "Forbidden", exception.getMessage(), context.path()));
        });

        app.exception(NoSuchElementException.class, (exception, context) -> {
            context.status(404).json(new ApiErrorResponse(404, "Not Found", exception.getMessage(), context.path()));
        });

        app.exception(PayloadTooLargeException.class, (exception, context) -> {
            context.status(413).json(new ApiErrorResponse(413, "Payload Too Large", exception.getMessage(), context.path()));
        });

        app.exception(UnsupportedMediaTypeException.class, (exception, context) -> {
            context.status(415)
                    .json(new ApiErrorResponse(415, "Unsupported Media Type", exception.getMessage(), context.path()));
        });

        app.exception(IllegalArgumentException.class, (exception, context) -> {
            context.status(422)
                    .json(new ApiErrorResponse(422, "Unprocessable Entity", exception.getMessage(), context.path()));
        });

        app.exception(Exception.class, (exception, context) -> {
            context.status(500)
                    .json(new ApiErrorResponse(500, "Internal Server Error", "Errore interno del server", context.path()));
        });
    }
}
