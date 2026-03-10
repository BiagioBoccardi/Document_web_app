package com.example.user_service.controller;

import com.example.user_service.models.User;
import com.example.user_service.service.UserService;

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public AuthResponse register(RegisterRequest request) {
        User utente = userService.register(
                request.nome(),
                request.email(),
                request.password(),
                request.isAdmin()
        );

        String token = userService.generateJwtToken(utente);
        return new AuthResponse(utente.getId(), utente.getEmail(), token);
    }

    public AuthResponse login(LoginRequest request) {
        User utente = userService.login(request.email(), request.password());
        String token = userService.generateJwtToken(utente);
        return new AuthResponse(utente.getId(), utente.getEmail(), token);
    }

    public ProfileResponse getProfile(long userId) {
        User utente = userService.getProfile(userId);
        return new ProfileResponse(
                utente.getId(),
                utente.getNome(),
                utente.getEmail(),
                utente.isAdmin()
        );
    }

<<<<<<< HEAD
    public ProfileResponse updateProfile(long userId, UpdateProfileRequest request) {
        User utente = userService.updateProfile(userId, request.nome(), request.cognome());
=======
    public ProfileResponse updateProfile(int userId, UpdateProfileRequest request) {
        User utente = userService.updateProfile(userId, request.nome());
>>>>>>> a31fb12f1dc53f77edc7c168491f544774be7500
        return new ProfileResponse(
                utente.getId(),
                utente.getNome(),
                utente.getEmail(),
                utente.isAdmin()
        );
    }

    public record RegisterRequest(String nome, String email, String password, boolean isAdmin) {

    }

    public record LoginRequest(String email, String password) {

    }

    public record UpdateProfileRequest(String nome) {

    }

    public record AuthResponse(int id, String email, String token) {

    }

    public record ProfileResponse(int id, String nome, String email, boolean isAdmin) {

    }
}