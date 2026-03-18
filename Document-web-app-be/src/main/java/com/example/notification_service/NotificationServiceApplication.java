package com.example.notification_service;

import com.example.notification_service.config.HibernateUtil;
import com.example.notification_service.controller.NotificationController;
import com.example.notification_service.messaging.EventConsumer;
import com.example.notification_service.repository.NotificationRepository;
import com.example.notification_service.service.NotificationService;
import com.example.notification_service.service.NotificationTemplateService;

import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.put;
import io.javalin.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotificationServiceApplication {
    private final Javalin app;
    private final NotificationController notificationController;
    private final EventConsumer eventConsumer;

    public NotificationServiceApplication() {
        NotificationRepository repository = new NotificationRepository();
        NotificationService notificationService = new NotificationService(repository);
        NotificationTemplateService templateService = new NotificationTemplateService();
        this.notificationController = new NotificationController(notificationService);
        this.eventConsumer = new EventConsumer(notificationService, templateService);

        // Configurazione Javalin
        this.app = Javalin.create(config -> {
            // Configurazione CORS specifica per il frontend React
            config.plugins.enableCors(cors -> {
                cors.add(it -> it.allowHost("http://localhost:5173", "http://localhost"));
            });
            
            config.requestLogger.http((ctx, ms) -> {
                log.info("{} {} - status: {} in {}ms", ctx.method(), ctx.path(), ctx.status(), ms);
            });
        });

        setupMiddleware();
        setupRoutes();
    }
    
    // Metodo utilizzato dentro i test
    public Javalin getJavalinInstance() {
        return this.app;
    }

    private void setupMiddleware() {
        app.before("/api/*", ctx -> {
            // Requisito di sicurezza: Header X-User-ID obbligatorio
            if (ctx.header("X-User-ID") == null) {
                log.warn("Tentativo di accesso senza header identificativo su: {}", ctx.path());
                throw new io.javalin.http.UnauthorizedResponse("Missing Authentication Header");
            }
        });
    }

    private void setupRoutes() {
        // Health check per Docker
        app.get("/health", ctx -> ctx.status(HttpStatus.OK).result("UP"));

        app.routes(() -> {
            path("/api/v1/notifications", () -> {
                get(notificationController::listNotifications);   
                post(notificationController::createNotification);
                put("/{id}/read", notificationController::markAsRead);   
                delete("/{id}", notificationController::deleteNotification); 
            });
        });
    }

    public void start(int port) {
        log.info("Inizializzazione Notification Service sulla porta {}...", port);
        
        // Inizializza Hibernate
        HibernateUtil.getSessionFactory();
        
        // Avvia l'ascolto degli eventi RabbitMQ
        eventConsumer.startListening();

        app.start(port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Chiusura in corso del Notification Service...");
            this.stop();
        }));
    }

    public void stop() {
        eventConsumer.stopListening();
        app.stop();
        HibernateUtil.shutdown();
        log.info("Notification Service arrestato correttamente.");
    }
}
