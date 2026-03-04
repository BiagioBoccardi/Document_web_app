package com.example.document_service.http;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.example.document_service.exception.UnauthorizedException;

import io.javalin.http.Context;

public class DocumentAuthMiddleware {
    public static final String USER_ID_CONTEXT_KEY = "authenticatedUserId";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String GATEWAY_USER_ID_HEADER = "X-User-Id";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWTVerifier jwtVerifier;

    public DocumentAuthMiddleware(String jwtSecret, String jwtIssuer) {
        Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
        this.jwtVerifier = (jwtIssuer == null || jwtIssuer.isBlank())
                ? JWT.require(algorithm).build()
                : JWT.require(algorithm).withIssuer(jwtIssuer).build();
    }

    public void authenticate(Context context) {
        Long userIdFromToken = extractUserIdFromBearerToken(context.header(AUTHORIZATION_HEADER));
        if (userIdFromToken != null) {
            context.attribute(USER_ID_CONTEXT_KEY, userIdFromToken);
            return;
        }

        String gatewayUserId = context.header(GATEWAY_USER_ID_HEADER);
        if (gatewayUserId != null && !gatewayUserId.isBlank()) {
            context.attribute(USER_ID_CONTEXT_KEY, parsePositiveUserId(gatewayUserId, "Header X-User-Id non valido"));
            return;
        }

        throw new UnauthorizedException("Token JWT mancante o non valido");
    }

    private Long extractUserIdFromBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthorizedException("Authorization header non valido");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new UnauthorizedException("Token JWT mancante");
        }

        try {
            DecodedJWT decodedJWT = jwtVerifier.verify(token);
            Long claimUserId = decodedJWT.getClaim("userId").isNull() ? null : decodedJWT.getClaim("userId").asLong();
            if (claimUserId != null && claimUserId > 0) {
                return claimUserId;
            }

            String subject = decodedJWT.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new UnauthorizedException("Claim userId o subject assente nel token JWT");
            }

            return parsePositiveUserId(subject, "Subject JWT non valido");
        } catch (JWTVerificationException exception) {
            throw new UnauthorizedException("Token JWT non valido");
        }
    }

    private long parsePositiveUserId(String rawUserId, String errorMessage) {
        try {
            long userId = Long.parseLong(rawUserId);
            if (userId <= 0) {
                throw new UnauthorizedException(errorMessage);
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException(errorMessage);
        }
    }
}
