package com.example.user_service.controller;

import com.example.user_service.model.Gruppo;
import com.example.user_service.service.GruppoService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Optional;

public class GruppoController {

    private final GruppoService gruppoService;

    public GruppoController(GruppoService gruppoService) {
        this.gruppoService = gruppoService;
    }

    public void getAll(Context ctx) {
        ctx.json(gruppoService.getAllGruppi());
    }

    public void getOne(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        Optional<Gruppo> gruppo = gruppoService.getGruppoById(id);
        if (gruppo.isPresent()) {
            ctx.json(gruppo.get());
        } else {
            ctx.status(HttpStatus.NOT_FOUND).result("Gruppo non trovato");
        }
    }

    public void create(Context ctx) {
        GruppoDto input = ctx.bodyAsClass(GruppoDto.class);

        ctx.status(HttpStatus.CREATED).result("Gruppo creato con successo");
    }

    public void delete(Context ctx) {
        long id = ctx.pathParamAsClass("id", Long.class).get();
        gruppoService.deleteGruppo(id);
        ctx.status(HttpStatus.NO_CONTENT);
    }

    public static class GruppoDto {

        public String name;
    }
}
