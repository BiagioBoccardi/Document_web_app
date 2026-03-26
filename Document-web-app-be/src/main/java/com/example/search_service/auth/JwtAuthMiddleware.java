package com.example.search_service.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.search_service.config.DotenvConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse; // Importante: aggiunta questa classe
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtAuthMiddleware {

    private static final String JWT_SECRET = DotenvConfig.get("JWT_SECRET", "default-secret");

    public static void handle(Context ctx) {
        String authHeader = ctx.header("Authorization");

        // Controllo header: se manca o è errato, lanciamo l'eccezione di Javalin
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Tentativo di accesso senza token valido");
            throw new UnauthorizedResponse("Token di autenticazione mancante o non valido");
        }

        String token = authHeader.substring(7);

        try {
            Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
            String userId = jwt.getSubject();

            if (userId == null || userId.isBlank()) {
                throw new JWTVerificationException("userId (sub) mancante nel token");
            }

            ctx.attribute("userId", userId);
            log.debug("JWT valido per userId='{}'", userId);

        } catch (JWTVerificationException e) {
            log.warn("Token JWT non valido: {}", e.getMessage());
            // Anche qui, interrompiamo tutto con un'eccezione 401
            throw new UnauthorizedResponse("Token non valido o scaduto");
        }
    }
}