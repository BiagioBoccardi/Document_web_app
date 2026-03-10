package com.example.user_service.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.user_service.models.User;
import com.example.user_service.persistence.UserDao;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

public class UserService {

    private final UserDao userDao;
    private final String jwtSecret;

    public UserService(UserDao userDao, String jwtSecret) {
        this.userDao = userDao;
        this.jwtSecret = jwtSecret;
    }

    public User register(String nome, String cognome, String email, String plainPassword) {
        Optional<User> existingUser = userDao.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new IllegalStateException("Email già registrata");
        }

        User user = new User();
        user.setNome(nome);
        user.setCognome(cognome);
        user.setEmail(email);
        user.setPassword(BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray()));
        user.setAdmin(false);

        return userDao.save(user);
    }

    public String login(String email, String plainPassword) {
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenziali non valide"));

        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), user.getPassword());
        if (!result.verified) {
            throw new IllegalArgumentException("Credenziali non valide");
        }

        return generateJwtToken(user);
    }

    public User getProfile(long userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User non trovato"));
    }

    public User getProfileByEmail(String email) {
        return userDao.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User non trovato"));
    }

    public User updateProfile(long userId, String nome, String cognome) {
        User user = getProfile(userId);
        user.setNome(nome);
        user.setCognome(cognome);
        userDao.update(user);
        return user;
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