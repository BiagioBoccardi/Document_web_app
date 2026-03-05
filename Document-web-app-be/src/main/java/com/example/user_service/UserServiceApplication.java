package com.example.user_service;

import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.example.user_service.controller.UserController;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.UserService;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class UserServiceApplication {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_JWT_SECRET = "change-me-in-production";
    private static final String PERSISTENCE_UNIT_NAME = "user-service-pu";

    public static void main(String[] args) {
        start();
    }

    public static void start() {
        int port = readPort();
        String jwtSecret = readEnv("JWT_SECRET", DEFAULT_JWT_SECRET);

        EntityManagerFactory emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);

        UserRepository userRepository = new UserRepository(emf);
        UserService userService = new UserService(userRepository, jwtSecret);
        UserController userController = new UserController(userService);

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.plugins.enableCors(cors -> cors.add(it -> it.anyHost()));
        }).start(port);

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("message", e.getMessage()));
        });

        app.exception(IllegalStateException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT).json(Map.of("message", e.getMessage()));
        });

        app.exception(UnauthorizedResponse.class, (e, ctx) -> {
            ctx.status(HttpStatus.UNAUTHORIZED).json(Map.of("message", e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("message", "Internal Server Error"));
        });

        app.get("/health", ctx -> ctx.json(Map.of("status", "UP", "service", "user-service")));

        app.post("/api/v1/users/register", ctx -> {
            var request = ctx.bodyAsClass(UserController.RegisterRequest.class);
            ctx.status(HttpStatus.CREATED).json(userController.register(request));
        });

        app.post("/api/v1/users/login", ctx -> {
            var request = ctx.bodyAsClass(UserController.LoginRequest.class);
            ctx.json(userController.login(request));
        });

        app.before("/api/v1/users/me", ctx -> authenticate(ctx, jwtSecret));

        app.get("/api/v1/users/me", ctx -> {
            int userId = ctx.attribute("userId");
            ctx.json(userController.getProfile(userId));
        });

        app.put("/api/v1/users/me", ctx -> {
            int userId = ctx.attribute("userId");
            var request = ctx.bodyAsClass(UserController.UpdateProfileRequest.class);
            ctx.json(userController.updateProfile(userId, request));
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            app.stop();
            if (emf.isOpen()) {
                emf.close();
            }
        }));
    }

    private static void authenticate(Context ctx, String jwtSecret) {
        String header = ctx.header("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Missing or invalid Authorization header");
        }

        String token = header.substring(7);
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);

            ctx.attribute("userId", Integer.parseInt(jwt.getSubject()));
            ctx.attribute("role", jwt.getClaim("role").asString());
        } catch (Exception e) {
            throw new UnauthorizedResponse("Invalid or expired token");
        }
    }

    private static int readPort() {
        String value = System.getenv("APP_PORT");
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
            }
        }
        return DEFAULT_PORT;
    }

    private static String readEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }
}
