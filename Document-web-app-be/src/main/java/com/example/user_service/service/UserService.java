package com.example.user_service.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.mindrot.jbcrypt.BCrypt;

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
        user.setPassword(BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
        user.setAdmin(false);

        return userRepository.save(user);
    }

    public String login(String email, String plainPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenziali non valide"));

        if (!BCrypt.checkpw(plainPassword, user.getPassword())) {
            throw new IllegalArgumentException("Credenziali non valide");
        }

        return generateJwtToken(user);
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

    private String generateJwtToken(User utente) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

        long iat = Instant.now().getEpochSecond();
        long exp = iat + 3600;
        String payloadJson = String.format(
                "{\"sub\":\"%d\",\"email\":\"%s\",\"admin\":%s,\"iat\":%d,\"exp\":%d}",
                utente.getId(),
                escapeJson(utente.getEmail()),
                utente.isAdmin(),
                iat,
                exp
        );

        String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String unsignedToken = header + "." + payload;
        String signature = sign(unsignedToken, jwtSecret);

        return unsignedToken + "." + signature;
    }

    private String sign(String data, String secret) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            byte[] signature = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(signature);
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("Errore nella generazione del JWT", ex);
        }
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}