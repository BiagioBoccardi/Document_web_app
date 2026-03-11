package com.example;

import com.example.document_service.DocumentServiceApplication;
import com.example.notification_service.NotificationServiceApplication;
import com.example.user_service.UserServiceApplication;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class App {
    private static final Logger log = LoggerFactory.getLogger(App.class);
    public static void main(String[] args) {
        String serviceType = System.getenv("SERVICE_TYPE");
        // porta di default dei microservizi, può essere sovrascritta da una variabile d'ambiente PORT
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080")); 

        if (serviceType == null) {
            log.error("ERRORE: Variabile d'ambiente SERVICE_TYPE non impostata!");
            System.exit(1);
        }

        log.info("Avvio del microservizio: " + serviceType);

        switch (serviceType) {
            case "USER_SERVICE" -> startUserService();
            case "NOTIFICATION_SERVICE" -> startNotificationService(port); // Passiamo la porta qui
            case "SEARCH_SERVICE" -> startSearchService();
            case "DOCUMENT_SERVICE" -> startDocumentService();
            default -> {
                log.error("Servizio sconosciuto: " + serviceType);
                System.exit(1);
            }
        }
    }

    private static void startUserService() {
        log.info("Inizializzazione User Service su Postgres...");
        UserServiceApplication.start();
    }

    private static void startNotificationService(int port) {
        log.info("Inizializzazione Notification Service su RabbitMQ...");
        // Creiamo l'istanza e avviamo passando la porta corretta
        NotificationServiceApplication notificationApp = new NotificationServiceApplication();
        notificationApp.start(port);
    }

    private static void startSearchService() {
        log.info("Inizializzazione Search Service su Elasticsearch...");
    }

    private static void startDocumentService() {
        log.info("Inizializzazione Document Service...");
        DocumentServiceApplication.start();
    }
}