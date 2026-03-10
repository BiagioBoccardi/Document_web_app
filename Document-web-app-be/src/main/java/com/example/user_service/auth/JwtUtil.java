package com.example.user_service.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.user_service.models.User;

import java.util.Date;

public class JwtUtil {
    // Chiave segreta. In un'app reale, dovrebbe essere in un file di configurazione sicuro.
    private static final String SECRET = "la-tua-chiave-segreta-super-difficile-da-indovinare";
    private static final long EXPIRATION_TIME = 86_400_000; // 1 giorno in millisecondi

    public static String generateToken(User user) {
        return JWT.create()
                .withSubject(String.valueOf(user.getId()))
                .withClaim("email", user.getEmail())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(Algorithm.HMAC256(SECRET));
    }
}
