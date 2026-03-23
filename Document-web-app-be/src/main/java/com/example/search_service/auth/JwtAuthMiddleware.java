package com.example.search_service.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.search_service.config.DotenvConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtAuthMiddleware {

    private static final String JWT_SECRET = DotenvConfig.get("JWT_SECRET", "default-secret");

    public static void handle(Context ctx) {
        String authHeader = ctx.header("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json("{\"error\": \"Token di autenticazione mancante o non valido\"}");
            ctx.skipRemainingHandlers();
            return;
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
            ctx.status(HttpStatus.UNAUTHORIZED)
               .json("{\"error\": \"Token non valido o scaduto\"}");
            ctx.skipRemainingHandlers();
        }
    }
}
