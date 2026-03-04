package com.example.user_service.controller;

import com.example.user_service.model.UtenteModel;
import com.example.user_service.service.UserService;

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public AuthResponse register(RegisterRequest request) {
        UtenteModel utente = userService.register(
                request.nome(),
                request.cognome(),
                request.email(),
                request.password()
        );

        String token = userService.login(request.email(), request.password());
        return new AuthResponse(utente.getId(), utente.getEmail(), token);
    }

    public AuthResponse login(LoginRequest request) {
        String token = userService.login(request.email(), request.password());
        UtenteModel utente = userService.getProfileByEmail(request.email());
        return new AuthResponse(utente.getId(), utente.getEmail(), token);
    }

    public ProfileResponse getProfile(int userId) {
        UtenteModel utente = userService.getProfile(userId);
        return new ProfileResponse(
                utente.getId(),
                utente.getNome(),
                utente.getCognome(),
                utente.getEmail(),
                utente.isAdmin()
        );
    }

    public ProfileResponse updateProfile(int userId, UpdateProfileRequest request) {
        UtenteModel utente = userService.updateProfile(userId, request.nome(), request.cognome());
        return new ProfileResponse(
                utente.getId(),
                utente.getNome(),
                utente.getCognome(),
                utente.getEmail(),
                utente.isAdmin()
        );
    }

    public record RegisterRequest(String nome, String cognome, String email, String password) {

    }

    public record LoginRequest(String email, String password) {

    }

    public record UpdateProfileRequest(String nome, String cognome) {

    }

    public record AuthResponse(int id, String email, String token) {

    }

    public record ProfileResponse(int id, String nome, String cognome, String email, boolean isAdmin) {

    }
}
