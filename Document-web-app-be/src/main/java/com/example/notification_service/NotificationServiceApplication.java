package com.example.notification_service;

import com.example.notification_service.config.HibernateUtil;
import com.example.notification_service.controller.NotificationController;
import com.example.notification_service.repository.NotificationRepository;
import com.example.notification_service.service.NotificationService;
import com.example.notification_service.messaging.EventConsumer;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotificationServiceApplication {
    private final Javalin app;
    private final NotificationController notificationController;
    private final EventConsumer eventConsumer;

    public NotificationServiceApplication() {
        NotificationRepository repository = new NotificationRepository();
        NotificationService service = new NotificationService(repository);
        this.notificationController = new NotificationController(service);
        
        // Lo stub del consumer per RabbitMQ
        this.eventConsumer = new EventConsumer(service);

        // 2. Configurazione Javalin
        this.app = Javalin.create(config -> {
            // Logging delle richieste per osservabilità
            config.requestLogger.http((ctx, ms) -> {
                log.info("{} {} - Stato: {} in {}ms", ctx.method(), ctx.path(), ctx.status(), ms);
            });

            // Configurazione CORS (se necessaria per il frontend)
            // config.bundledPlugins.enableCors(cors -> {
            //     cors.addRule(it -> it.anyHost());
            // });
        });

        setupMiddleware();
        setupRoutes();
    }

    /**
     * Configura i filtri di sicurezza globali.
     */
    private void setupMiddleware() {
        app.before("/api/*", ctx -> {
            // Verifica la presenza dell'header X-User-ID (iniettato dal Gateway)
            String userId = ctx.header("X-User-ID");
            if (userId == null || userId.isBlank()) {
                log.warn("Accesso negato: Header X-User-ID mancante per il path {}", ctx.path());
                throw new io.javalin.http.UnauthorizedResponse("Autenticazione richiesta");
            }
        });
    }

    /**
     * Definizione degli endpoint API.
     */
    private void setupRoutes() {
        // Endpoint di Health Check
        app.get("/health", ctx -> ctx.status(HttpStatus.OK).result("UP"));

        // Gruppo API Notifiche v1
        app.routes(() -> {
            app.path("/api/v1/notifications", () -> {
                app.get(notificationController::listNotifications);          // GET lista
                app.put("/{id}/read", notificationController::markAsRead);   // PUT mark as read
                app.delete("/{id}", notificationController::deleteNotification); // DELETE
            });
        });
    }

    /**
     * Avvia il servizio, il database e i consumer di messaggistica.
     */
    public void start(int port) {
        log.info("Avvio del Notification Service sulla porta {}...", port);
        
        // Inizializza il pool di connessioni Hibernate
        HibernateUtil.getSessionFactory();
        
        // Avvia l'ascolto dei messaggi RabbitMQ in un thread separato
        // eventConsumer.startListening();

        app.start(port);
    }

    /**
     * Arresto pulito delle risorse.
     */
    public void stop() {
        log.info("Arresto del Notification Service...");
        eventConsumer.stopListening();
        app.stop();
        HibernateUtil.shutdown();
    }
}
