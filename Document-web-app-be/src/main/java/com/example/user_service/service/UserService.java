package com.example.user_service.service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.mindrot.jbcrypt.BCrypt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;

public class UserService {

    private final UserRepository userRepository;
    private final String jwtSecret;

    public UserService(UserRepository userRepository, String jwtSecret) {
        this.userRepository = userRepository;
        this.jwtSecret = jwtSecret;
    }

    public User register(String nome, String cognome, String email, String plainPassword) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new IllegalStateException("Email già registrata");
        }

        User user = new User();
        user.setNome(nome);
        user.setCognome(cognome);
        user.setEmail(email);
        user.setPasswordHash(BCrypt.hashpw(plainPassword, BCrypt.gensalt(12))); 

        return userRepository.save(user);
    }

    public User login(String email, String plainPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenziali non valide"));

        if (!BCrypt.checkpw(plainPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Credenziali non valide");
        }

        return user;
    }

    public User getProfile(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User non trovato"));
    }

    public User getProfileByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User non trovato"));
    }

    public User updateProfile(int userId, String nome, String cognome) {
        User user = getProfile(userId);
        user.setNome(nome);
        user.setCognome(cognome);
        return userRepository.update(user);
    }

    public String generateJwtToken(User utente) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            return JWT.create()
                    .withSubject(String.valueOf(utente.getId()))
                    .withClaim("email", utente.getEmail())
                    .withClaim("role", utente.isAdmin() ? "ADMIN" : "USER") 
                    .withIssuedAt(Date.from(Instant.now()))
                    .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                    .sign(algorithm);
        } catch (Exception ex) {
            throw new IllegalStateException("Errore nella generazione del token JWT", ex);
        }
    }
}
