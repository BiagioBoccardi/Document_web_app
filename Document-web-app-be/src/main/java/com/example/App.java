package com.example;

import com.example.document_service.DocumentServiceApplication;
import com.example.notification_service.NotificationServiceApplication;
import com.example.user_service.UserServiceApplication;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class App {

    public static void main(String[] args) {
        String serviceType = System.getenv("SERVICE_TYPE");

        if (serviceType == null) {
            log.error("ERRORE: Variabile d'ambiente SERVICE_TYPE non impostata!");
            System.exit(1);
        }

        log.info("Avvio del microservizio: {}", serviceType);

        switch (serviceType) {
            case "USER_SERVICE"         -> startUserService();
            case "NOTIFICATION_SERVICE" -> startNotificationService();
            case "SEARCH_SERVICE"       -> startSearchService();
            case "DOCUMENT_SERVICE"     -> startDocumentService();
            default -> {
                log.error("Servizio sconosciuto: {}", serviceType);
                System.exit(1);
            }
        }
    }

    private static void startUserService() {
        log.info("Inizializzazione User Service su Postgres...");
        UserServiceApplication.start();
    }

    private static void startNotificationService() {
        log.info("Inizializzazione Notification Service su RabbitMQ...");
        NotificationServiceApplication notificationApp = new NotificationServiceApplication();
        // notificationApp.start(); 
    }

    private static void startSearchService() {
        log.info("Inizializzazione Search Service su Elasticsearch...");
        // SearchServiceApplication.start(); // da implementare
    }

    private static void startDocumentService() {
        log.info("Inizializzazione Document Service...");
        DocumentServiceApplication.start();
    }
}