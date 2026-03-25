package com.example.user_service.utils;

import java.util.Date;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

public class JwtUtils {
    private static final String SECRET = System.getenv().getOrDefault("JWT_SECRET", "super-segreto-di-bonelle-2026");
    private static final String ISSUER = "document-web-app";
    private static final long EXPIRATION_TIME = 86400000; // 24 ore in millisecondi

    public static String generateToken(int userId, String email) {
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(String.valueOf(userId))
                .withClaim("email", email)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .sign(Algorithm.HMAC256(SECRET));
    }
}
