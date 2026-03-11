package com.example.user_service.controller;

import com.example.user_service.dto.CreateGruppoRequest;
import com.example.user_service.dto.MembroRequest;
import com.example.user_service.model.Gruppo;
import com.example.user_service.model.User;
import com.example.user_service.service.GruppoService;
import com.example.user_service.service.UserService;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class GruppoController {

    private final GruppoService gruppoService;
    private final UserService userService;

    public GruppoController(GruppoService gruppoService, UserService userService) {
        this.gruppoService = gruppoService;
        this.userService = userService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/gruppi", this::createGruppo);
        app.get("/api/gruppi", this::getAllGruppi);
        app.get("/api/gruppi/{id}", this::getGruppoById);
        app.delete("/api/gruppi/{id}", this::deleteGruppo);
        app.post("/api/gruppi/{id}/membri", this::addMembro);
        app.delete("/api/gruppi/{id}/membri", this::removeMembro);
    }

    // POST /api/gruppi
    private void createGruppo(Context ctx) {
        CreateGruppoRequest body = ctx.bodyAsClass(CreateGruppoRequest.class);

        if (body.name == null || body.name.isBlank() ) {
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }

        try {
            User owner = userService.getProfile(body.ownerId);
            Gruppo gruppo = gruppoService.createGruppo(body.name, owner);
            ctx.status(HttpStatus.CREATED).json(gruppo);
        } catch (IllegalArgumentException ex) {
            ctx.status(HttpStatus.NOT_FOUND);
        }
    }

    // GET /api/gruppi
    private void getAllGruppi(Context ctx) {
        ctx.status(HttpStatus.OK).json(gruppoService.getAllGruppi());
    }

    // GET /api/gruppi/{id}
    private void getGruppoById(Context ctx) {
        int gruppoId = parseId(ctx);
        if (gruppoId == -1) return;

        gruppoService.getGruppoById(gruppoId)
                .ifPresentOrElse(
                        g -> ctx.status(HttpStatus.OK).json(g),
                        () -> ctx.status(HttpStatus.NOT_FOUND)
                );
    }

    // DELETE /api/gruppi/{id}
    private void deleteGruppo(Context ctx) {
        int gruppoId = parseId(ctx);
        if (gruppoId == -1) return;

        try {
            gruppoService.deleteGruppo(gruppoId);
            ctx.status(HttpStatus.NO_CONTENT);
        } catch (IllegalArgumentException ex) {
            ctx.status(HttpStatus.NOT_FOUND);
        }
    }

    // POST /api/gruppi/{id}/membri
    private void addMembro(Context ctx) {
        int gruppoId = parseId(ctx);
        if (gruppoId == -1) return;

        MembroRequest body = ctx.bodyAsClass(MembroRequest.class);

        try {
            User user = userService.getProfile(body.userId);
            Gruppo gruppo = gruppoService.addMembro(gruppoId, user);
            ctx.status(HttpStatus.OK).json(gruppo);
        } catch (IllegalArgumentException ex) {
            ctx.status(HttpStatus.NOT_FOUND);
        } catch (IllegalStateException ex) {
            ctx.status(HttpStatus.CONFLICT);
        }
    }

    // DELETE /api/gruppi/{id}/membri
    private void removeMembro(Context ctx) {
        int gruppoId = parseId(ctx);
        if (gruppoId == -1) return;

        MembroRequest body = ctx.bodyAsClass(MembroRequest.class);

        try {
            User user = userService.getProfile(body.userId);
            Gruppo gruppo = gruppoService.removeMembro(gruppoId, user);
            ctx.status(HttpStatus.OK).json(gruppo);
        } catch (IllegalArgumentException ex) {
            ctx.status(HttpStatus.NOT_FOUND);
        } catch (IllegalStateException ex) {
            ctx.status(HttpStatus.CONFLICT);
        }
    }

    // --- Helper ---
    private int parseId(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException ex) {
            ctx.status(HttpStatus.BAD_REQUEST);
            return -1;
        }
    }
}