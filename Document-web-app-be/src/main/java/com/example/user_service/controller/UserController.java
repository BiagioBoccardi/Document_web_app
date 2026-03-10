package com.example.user_service.controller;

import com.example.user_service.model.User;
import com.example.user_service.service.UserService;

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // --- CLASSI DI RICHIESTA (DTO) ---
    public static class RegisterRequest {
        public String nome;
        public String cognome;
        public String email;
        public String password;
    }

    public static class LoginRequest {
        public String email;
        public String password;
    }

    public static class UpdateProfileRequest {
        public String nome;
        public String cognome;
    }

    // --- CLASSI DI RISPOSTA (DTO) ---
    public static class AuthResponse {
        public String token;
        public User user;

        public AuthResponse(String token, User user) {
            this.token = token;
            this.user = user;
        }
    }

    public static class ProfileResponse {
        public int id;
        public String nome;
        public String cognome;
        public String email;
        public boolean isAdmin;

        public ProfileResponse(User user) {
            this.id = user.getId();
            this.nome = user.getNome();
            this.cognome = user.getCognome();
            this.email = user.getEmail();
            this.isAdmin = user.isAdmin();
        }
    }

    // --- METODI DEL CONTROLLER ---

    public AuthResponse register(RegisterRequest request) {
        User user = userService.register(request.nome, request.email, request.password);
        String token = userService.generateJwtToken(user);
        return new AuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userService.login(request.email, request.password);
        String token = userService.generateJwtToken(user);
        return new AuthResponse(token, user);
    }

    public ProfileResponse getProfile(int userId) {
        User user = userService.getProfile(userId);
        return new ProfileResponse(user);
    }

    public ProfileResponse updateProfile(int userId, UpdateProfileRequest request) {
        User user = userService.updateProfile(userId, request.nome, request.cognome);
        return new ProfileResponse(user);
    }
}