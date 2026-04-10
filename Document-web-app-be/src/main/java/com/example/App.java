package com.example;

import com.example.document_service.DocumentServiceApplication;
import com.example.notification_service.NotificationServiceApplication;
import com.example.search_service.SearchService;
import com.example.user_service.UserServiceApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

    public static void main(String[] args) {
        String serviceType = System.getenv().getOrDefault("SERVICE_TYPE", "ALL");
        int defaultPort = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        if (serviceType == null) {
            log.error("ERRORE: Variabile d'ambiente SERVICE_TYPE non impostata!");
            System.exit(1);
        }

        log.info("Configurazione rilevata - Servizio: {}, Porta base: {}", serviceType, defaultPort);

        switch (serviceType) {
            case "ALL" -> {
                log.info("MODALITÀ MULTI-SERVIZIO: Avvio di tutti i microservizi disponibili...");
                startUserService(8081);
                startDocumentService(8082);
                startSearchService(8083);
                startNotificationService(8084);
            }
            case "USER_SERVICE" -> startUserService(defaultPort);
            case "NOTIFICATION_SERVICE" -> startNotificationService(defaultPort);
            case "SEARCH_SERVICE" -> startSearchService(defaultPort);
            case "DOCUMENT_SERVICE" -> startDocumentService(defaultPort);
            default -> {
                log.error("Servizio sconosciuto: {}", serviceType);
                System.exit(1);
            }
        }
    }

    private static void startUserService(int port) {
        log.info("Inizializzazione User Service su Postgres...");
        UserServiceApplication.start(port);
    }

    private static void startNotificationService(int port) {
        log.info("Inizializzazione Notification Service su RabbitMQ...");
        NotificationServiceApplication notificationApplication = new NotificationServiceApplication();
        notificationApplication.start(port); 
    }

    private static void startSearchService(int port) {
        log.info("Inizializzazione Search Service su Qdrant...");
        SearchService.start(port);
    }

    private static void startDocumentService(int port) {
        log.info("Inizializzazione Document Service...");
        DocumentServiceApplication.start(port);
    }
}