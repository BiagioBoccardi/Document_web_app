package com.example.search_service.unit;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.search_service.auth.JwtAuthMiddleware;
import com.example.search_service.config.DotenvConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthMiddleware - Test unitari")
class JwtAuthMiddlewareTest {

    // Usa la stessa logica di DotenvConfig per garantire che il token
    // generato nei test sia verificabile dal middleware.
    private static final String SECRET = DotenvConfig.get("JWT_SECRET", "default-secret");
    private static final String TEST_USER_ID = "utente-test-uuid";

    @Mock
    private Context ctx;

    // ─────────────────────────────────────────────
    // Helper: genera un JWT valido
    // ─────────────────────────────────────────────
    private String validToken(String userId) {
        return JWT.create()
                .withSubject(userId)
                .withExpiresAt(new Date(System.currentTimeMillis() + 3_600_000))
                .sign(Algorithm.HMAC256(SECRET));
    }

    // ═════════════════════════════════════════════
    // handle()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("handle()")
    class Handle {

        @Test
        @DisplayName("OK: token valido — imposta userId come attributo del contesto")
        void shouldSetUserIdAttributeOnValidToken() {
            when(ctx.header("Authorization")).thenReturn("Bearer " + validToken(TEST_USER_ID));

            JwtAuthMiddleware.handle(ctx);

            verify(ctx).attribute(eq("userId"), eq(TEST_USER_ID));
            verify(ctx, never()).status(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("KO: header Authorization assente — risponde 401, userId non impostato")
        void shouldReturn401WhenAuthHeaderMissing() {
            when(ctx.header("Authorization")).thenReturn(null);

            JwtAuthMiddleware.handle(ctx);

            verify(ctx).status(HttpStatus.UNAUTHORIZED);
            verify(ctx, never()).attribute(eq("userId"), any());
        }

        @Test
        @DisplayName("KO: header non inizia con 'Bearer ' — risponde 401")
        void shouldReturn401WhenHeaderIsNotBearer() {
            when(ctx.header("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            JwtAuthMiddleware.handle(ctx);

            verify(ctx).status(HttpStatus.UNAUTHORIZED);
            verify(ctx, never()).attribute(eq("userId"), any());
        }

        @Test
        @DisplayName("KO: token con firma non valida — risponde 401")
        void shouldReturn401WhenSignatureIsInvalid() {
            String fakeToken = JWT.create()
                    .withSubject(TEST_USER_ID)
                    .sign(Algorithm.HMAC256("chiave-sbagliata-diversa-dal-secret"));

            when(ctx.header("Authorization")).thenReturn("Bearer " + fakeToken);

            JwtAuthMiddleware.handle(ctx);

            verify(ctx).status(HttpStatus.UNAUTHORIZED);
            verify(ctx, never()).attribute(eq("userId"), any());
        }

        @Test
        @DisplayName("KO: token scaduto — risponde 401")
        void shouldReturn401WhenTokenExpired() {
            String expiredToken = JWT.create()
                    .withSubject(TEST_USER_ID)
                    .withExpiresAt(new Date(System.currentTimeMillis() - 1000))
                    .sign(Algorithm.HMAC256(SECRET));

            when(ctx.header("Authorization")).thenReturn("Bearer " + expiredToken);

            JwtAuthMiddleware.handle(ctx);

            verify(ctx).status(HttpStatus.UNAUTHORIZED);
            verify(ctx, never()).attribute(eq("userId"), any());
        }

        @Test
        @DisplayName("KO: token senza claim 'sub' — risponde 401")
        void shouldReturn401WhenSubClaimMissing() {
            String tokenWithoutSub = JWT.create()
                    .withClaim("email", "utente@example.com")
                    .withExpiresAt(new Date(System.currentTimeMillis() + 3_600_000))
                    .sign(Algorithm.HMAC256(SECRET));

            when(ctx.header("Authorization")).thenReturn("Bearer " + tokenWithoutSub);

            JwtAuthMiddleware.handle(ctx);

            verify(ctx).status(HttpStatus.UNAUTHORIZED);
            verify(ctx, never()).attribute(eq("userId"), any());
        }
    }
}
