package com.example.user_service;

import com.example.user_service.config.HibernateUtil;
import com.example.user_service.controller.GruppoController;
import com.example.user_service.controller.UserController;
import com.example.user_service.repository.GruppoRepository;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.GruppoService;
import com.example.user_service.service.UserService;

import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.SessionFactory;

@Slf4j
public class UserServiceApplication {

    private static final int DEFAULT_PORT = 8081;

    private UserServiceApplication() {}

    public static void start() {
        start(readPort());
    }

    public static void start(int port) {
        log.info("Inizializzazione User Service sulla porta {}...", port);

        // --- Hibernate ---
        // Usiamo l'utility invece di creare la configurazione qui a mano
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();

        // --- Repository ---
        UserRepository userRepository = new UserRepository(sessionFactory);
        GruppoRepository gruppoRepository = new GruppoRepository(sessionFactory);

        // --- Service ---
        UserService userService = new UserService(userRepository);
        GruppoService gruppoService = new GruppoService(gruppoRepository);

        // --- Controller ---
        UserController userController = new UserController(userService);
        GruppoController gruppoController = new GruppoController(gruppoService, userService);

        // --- Javalin 5x ---
        Javalin app = Javalin.create(config -> {
            config.plugins.enableCors(cors -> {
                cors.add(it -> it.allowHost("http://localhost:5173", "http://localhost"));
            });
        }).start(port);

        log.info("User Service avviato sulla porta {}", port);

        // --- Routes ---
        userController.registerRoutes(app);
        gruppoController.registerRoutes(app);

        // --- Shutdown hook ---
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Spegnimento User Service...");
            app.stop();
            HibernateUtil.shutdown();
        }));
    }

    private static int readPort() {
        String value = readEnv("APP_PORT", String.valueOf(DEFAULT_PORT));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return DEFAULT_PORT;
        }
    }

    private static String readEnv(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}