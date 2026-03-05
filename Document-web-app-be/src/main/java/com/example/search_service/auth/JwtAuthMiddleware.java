
package com.example.search_service.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.example.search_service.config.DotenvConfig;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;

public class JwtAuthMiddleware {

    // Leggiamo il segreto dal file .env
    private static final String SECRET = DotenvConfig.get("JWT_SECRET", "default_secret_change_me");
    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET);
    private static final JWTVerifier verifier = JWT.require(algorithm).build();

    public static void handle(Context ctx) {
        // 1. Estrazione dell'header Authorization (Bearer <token>)
        String authHeader = ctx.header("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Token mancante o formato non valido");
        }

        try {
            // 2. Verifica del token
            String token = authHeader.substring(7);
            DecodedJWT jwt = verifier.verify(token);

            // 3. REGOLA D'ORO #2: Estrazione userId per isolamento dati
            String userId = jwt.getClaim("userId").asString();
            if (userId == null) {
                userId = jwt.getSubject(); 
            }

            // 4. Salvataggio nel contesto della richiesta
            ctx.attribute("userId", userId);

        } catch (Exception e) {
            throw new UnauthorizedResponse("Token non valido o scaduto");
        }
    }
}
