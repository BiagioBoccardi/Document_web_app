package com.example.user_service.controller;

import java.util.List;
import java.util.Map;

import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.model.User;
import com.example.user_service.service.UserService;
import com.example.user_service.utils.JwtUtils;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void registerRoutes(Javalin app) {
        app.post("/api/v1/users/sign-up", this::register);
        app.post("/api/v1/users/sign-in", this::login);
        app.get("/api/v1/users", this::findAllUsers);
    }

    // POST /api/v1/users/sign-up
    private void register(Context ctx) {
        RegisterRequest body = ctx.bodyAsClass(RegisterRequest.class);

        if (body.nome == null || body.nome.isBlank() ||
            body.email == null || body.email.isBlank() ||
            body.passwordHash == null || body.passwordHash.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            return;
        }

        try {
            User user = userService.register(body.nome, body.email, body.passwordHash);

            user.setPasswordHash(null);

            ctx.status(HttpStatus.CREATED).json(user);
        } catch (IllegalStateException ex) {
            ctx.status(HttpStatus.CONFLICT).json(Map.of("error", ex.getMessage()));
        }
    }

    // POST /api/v1/users/sign-in
    private void login(Context ctx) {
        LoginRequest body = ctx.bodyAsClass(LoginRequest.class);

        if (body.email == null || body.email.isBlank() || 
            body.passwordHash == null || body.passwordHash.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            return; 
        }

        try {
            User user = userService.login(body.email, body.passwordHash); 

            if (user == null) {
                ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Credenziali non valide"));
                return;
            }

            // Genera il Token JWT
            String token = JwtUtils.generateToken(user.getId(), user.getEmail());

            user.setPasswordHash(null);

            ctx.status(HttpStatus.OK).json(java.util.Map.of(
                "user", user,
                "token", token
            ));
            log.info("Login effettuato con successo per: {}", user.getEmail());
        } catch (IllegalArgumentException ex) {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("error", "Credenziali non valide"));
        }
    }

    // GET /api/v1/users
    private void findAllUsers(Context ctx) {
        List<User> users = userService.getAllUsers();
        ctx.status(HttpStatus.OK).json(users);
    }
}