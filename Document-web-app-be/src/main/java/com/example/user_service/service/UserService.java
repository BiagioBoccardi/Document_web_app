package com.example.user_service.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.mindrot.jbcrypt.BCrypt;

import com.example.user_service.model.UtenteModel;
import com.example.user_service.repository.UserRepository;

public class UserService {

    private final UserRepository userRepository;
    private final String jwtSecret;

    public UserService(UserRepository userRepository, String jwtSecret) {
        this.userRepository = userRepository;
        this.jwtSecret = jwtSecret;
    }

    public UtenteModel register(String nome, String cognome, String email, String plainPassword) {
        Optional<UtenteModel> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            throw new IllegalStateException("Email già registrata");
        }

        UtenteModel utente = new UtenteModel();
        utente.setNome(nome);
        utente.setCognome(cognome);
        utente.setEmail(email);
        utente.setPassword(BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
        utente.setAdmin(false);

        return userRepository.save(utente);
    }

    public String login(String email, String plainPassword) {
        UtenteModel utente = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenziali non valide"));

        if (!BCrypt.checkpw(plainPassword, utente.getPassword())) {
            throw new IllegalArgumentException("Credenziali non valide");
        }

        return generateJwtToken(utente);
    }

    public UtenteModel getProfile(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
    }

    public UtenteModel getProfileByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));
    }

    public UtenteModel updateProfile(int userId, String nome, String cognome) {
        UtenteModel utente = getProfile(userId);
        utente.setNome(nome);
        utente.setCognome(cognome);
        return userRepository.update(utente);
    }

    private String generateJwtToken(UtenteModel utente) {
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