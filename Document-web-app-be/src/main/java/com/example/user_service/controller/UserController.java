package com.example.user_service.controller;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.model.User;
import com.example.user_service.service.UserService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/v1/users/sign-up", this::signup);
        app.post("/api/v1/users/sign-in", this::signin);
    }

    // POST /api/users/register
    private void signup(Context ctx) {
        RegisterRequest body = ctx.bodyAsClass(RegisterRequest.class);

        if (body.nome == null || body.nome.isBlank() ||
            body.email == null || body.email.isBlank() ||
            body.passwordHash == null || body.passwordHash.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }

        try {
            User user = userService.register(body.nome, body.email, body.passwordHash);
            ctx.status(HttpStatus.CREATED).json(user);
        } catch (IllegalStateException ex) {
            ctx.status(HttpStatus.CONFLICT);
        }
    }

    // POST /api/users/login
    private void signin(Context ctx) {
        LoginRequest body = ctx.bodyAsClass(LoginRequest.class);

        if (body.email == null || body.email.isBlank() ||
            body.passwordHash == null || body.passwordHash.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }

        try {
            User user = userService.login(body.email, body.passwordHash);
            ctx.status(HttpStatus.OK).json(user);
        } catch (IllegalArgumentException ex) {
            ctx.status(HttpStatus.UNAUTHORIZED);
        }
    }
}