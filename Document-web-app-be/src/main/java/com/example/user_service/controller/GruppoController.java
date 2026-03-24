package com.example.user_service.controller;

import java.util.Map;

import com.example.user_service.dto.CreateGruppoRequest;
import com.example.user_service.dto.MembroRequest;
import com.example.user_service.model.Gruppo;
import com.example.user_service.service.GruppoService;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GruppoController {

    private final GruppoService gruppoService;

    public GruppoController(GruppoService gruppoService) {
        this.gruppoService = gruppoService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/v1/gruppi", this::createGruppo);
        app.get("/api/v1/gruppi", this::getAllGruppi);
        app.get("/api/v1/gruppi/{id}", this::getGruppoById);
        app.delete("/api/v1/gruppi/{id}", this::deleteGruppo);
        app.post("/api/v1/gruppi/{id}/membri", this::addMembro);
        app.delete("/api/v1/gruppi/{id}/membri", this::removeMembro);
    }

    // POST /api/gruppi
    private void createGruppo(Context ctx) {
        CreateGruppoRequest body = ctx.bodyAsClass(CreateGruppoRequest.class);

        if (body.name == null || body.name.isBlank() ) {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Nome gruppo obbligatorio"));
            return;
        }

        try {
            Gruppo gruppo = gruppoService.createGruppo(body.name, body.ownerId, body.members);
            ctx.status(HttpStatus.CREATED).json(gruppo);
        } catch (IllegalArgumentException ex) {
            ctx.status(HttpStatus.NOT_FOUND).json(java.util.Map.of("Errore: ", ex.getMessage()));
            log.error(ex.getMessage());
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
            log.error(ex.getMessage());
        }
    }

    // POST /api/gruppi/{id}/membri
    private void addMembro(Context ctx) {
        int gruppoId = parseId(ctx);
        if (gruppoId == -1) return;

        MembroRequest body = ctx.bodyAsClass(MembroRequest.class);

        try {
            
            Gruppo gruppo = gruppoService.addMembro(gruppoId, body.userId);
            ctx.status(HttpStatus.OK).json(gruppo);
        } catch (IllegalArgumentException ex) {
            ctx.status(HttpStatus.NOT_FOUND);
            log.error(ex.getMessage());
        } catch (IllegalStateException ex) {
            ctx.status(HttpStatus.CONFLICT);
            log.error(ex.getMessage());
        }
    }

    // DELETE /api/gruppi/{id}/membri
    private void removeMembro(Context ctx) {
        int gruppoId = parseId(ctx);
        if (gruppoId == -1) return;

        MembroRequest body = ctx.bodyAsClass(MembroRequest.class);

        try {
            Gruppo gruppo = gruppoService.removeMembro(gruppoId, body.userId);
            ctx.status(HttpStatus.OK).json(gruppo);
        } catch (IllegalArgumentException ex) {
            ctx.status(HttpStatus.NOT_FOUND);
            log.error(ex.getMessage());
        } catch (IllegalStateException ex) {
            ctx.status(HttpStatus.CONFLICT);
            log.error(ex.getMessage());
        }
    }

    // --- Helper ---
    private int parseId(Context ctx) {
        try {
            return Integer.parseInt(ctx.pathParam("id"));
        } catch (NumberFormatException ex) {
            ctx.status(HttpStatus.BAD_REQUEST);
            log.error(ex.getMessage());
            return -1;
        }
    }
}