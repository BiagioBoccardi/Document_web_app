package com.example.notification_service.controller;

import java.util.Map;
import java.util.UUID;


import com.example.notification_service.dto.NotificationRequest;
import com.example.notification_service.entity.Notification;
import com.example.notification_service.service.NotificationService;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    /**
     * GET /api/v1/notifications
     *
     */
    public void listNotifications(Context ctx) {
        int userId = getAuthenticatedUserId(ctx); 
        
        String status = ctx.queryParam("status");
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int size = ctx.queryParamAsClass("size", Integer.class).getOrDefault(10);

        var notifications = service.getUserNotifications(userId, status, page, size);
        ctx.json(notifications);
    }

    /**
     * POST /api/v1/notifications
     *
     */
    public void createNotification(Context ctx) {
        NotificationRequest dto = ctx.bodyAsClass(NotificationRequest.class);
        Notification created = service.createNotification(dto.getUserId(), dto.getMessaggio());
        ctx.status(HttpStatus.CREATED).json(created);
    }

    /**
     * PUT /api/v1/notifications/{id}/read
     *
     */
    public void markAsRead(Context ctx) {
        int userId = getAuthenticatedUserId(ctx);
        UUID notificationId = UUID.fromString(ctx.pathParam("id"));

        try {
            service.markAsRead(notificationId, userId); 
            ctx.status(HttpStatus.NO_CONTENT); 
        } catch (RuntimeException e) {
            log.warn("Tentativo di accesso non autorizzato o notifica non trovata: {}", notificationId);
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage())); //
        }
    }

    /**
     * DELETE /api/v1/notifications/{id}
     *
     */
    public void deleteNotification(Context ctx) {
        int userId = getAuthenticatedUserId(ctx);
        UUID notificationId = UUID.fromString(ctx.pathParam("id"));

        try {
            service.deleteNotification(notificationId, userId);
            ctx.status(HttpStatus.NO_CONTENT); // Idempotenza
        } catch (RuntimeException e) {
            ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Helper per estrarre l'ID utente.
     * In produzione, questo verrebbe dal JWT decodificato o da un header X-User-ID dal Gateway.
     */
    private int getAuthenticatedUserId(Context ctx) {
        String userIdHeader = ctx.header("X-User-ID"); 
        if (userIdHeader == null) {
            // Se non c'è header, lanciamo un 401 Unauthorized
            throw new io.javalin.http.UnauthorizedResponse("Utente non autenticato");
        }
        return Integer.parseInt(userIdHeader);
    }
}
