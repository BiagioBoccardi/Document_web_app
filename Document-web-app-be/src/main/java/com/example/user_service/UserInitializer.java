package com.example.user_service;

import java.util.Map;
import java.util.Optional;

import com.example.user_service.auth.JwtUtil;
import com.example.user_service.models.User;
import com.example.user_service.persistence.UserDao;
import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.user_service.persistence.HibernateUtil;
import io.javalin.Javalin;

public class UserInitializer {

    public static void init(Javalin app) {
        // Gestione chiusura session factory di Hibernate
        app.events(event -> {
            event.serverStopping(HibernateUtil::shutdown);
        });

        System.out.println("User Service avviato sulla porta 81");

        UserDao userDao = new UserDao(HibernateUtil.getSessionFactory());

        // Endpoint di test per verificare che il server risponda
        app.get("/health", ctx -> ctx.json("{\"status\": \"UP\"}"));

        // Endpoint di LOGIN REALE
        app.post("/api/v1/sign-in", ctx -> {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            Optional<User> userOpt = userDao.findByEmail(request.email);

            if (userOpt.isEmpty()) {
                ctx.status(401).json(Map.of("error", "Credenziali non valide"));
                return;
            }

            User user = userOpt.get();
            BCrypt.Result result = BCrypt.verifyer().verify(request.password.toCharArray(), user.getPassword());

            if (result.verified) {
                String token = JwtUtil.generateToken(user);
                ctx.json(Map.of("token", token, "user", user.forClient()));
            } else {
                ctx.status(401).json(Map.of("error", "Credenziali non valide"));
            }
        });

        // Endpoint di REGISTRAZIONE REALE
        app.post("/api/v1/sign-up", ctx -> {
            User newUser = ctx.bodyAsClass(User.class);

            // Controlla se l'utente esiste già
            if (userDao.findByEmail(newUser.getEmail()).isPresent()) {
                ctx.status(409).json(Map.of("error", "Email già registrata"));
                return;
            }

            // Hash della password prima di salvarla
            String hashedPassword = BCrypt.withDefaults().hashToString(12, newUser.getPassword().toCharArray());
            newUser.setPassword(hashedPassword);

            userDao.save(newUser);

            // Rimuoviamo la password prima di restituire l'utente
            ctx.status(201).json(newUser.forClient());
        });
    }

    // Classe di supporto per il parsing del JSON
    public static class LoginRequest {
        public String email;
        public String password;
    }
}