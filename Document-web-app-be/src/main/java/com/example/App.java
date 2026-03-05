package com.example;

import com.example.document_service.DocumentServiceApplication;
import com.example.user_service.UserServiceApplication;

public class App {
    public static void main(String[] args) {
        String serviceType = System.getenv("SERVICE_TYPE");

        if (serviceType == null) {
            System.err.println("ERRORE: Variabile d'ambiente SERVICE_TYPE non impostata!");
            System.exit(1);
        }

        System.out.println("Avvio del microservizio: " + serviceType);

        switch (serviceType) {
            case "USER_SERVICE" -> startUserService();
            case "NOTIFICATION_SERVICE" -> startNotificationService();
            case "SEARCH_SERVICE" -> startSearchService();
            case "DOCUMENT_SERVICE" -> startDocumentService();
            default -> {
                System.err.println("Servizio sconosciuto: " + serviceType);
                System.exit(1);
            }
        }
    }

    private static void startUserService() {
        // Qui inizializzi Hibernate per Postgres e i relativi endpoint
        System.out.println("Inizializzazione User Service su Postgres...");
        UserServiceApplication.start();
    }

    private static void startNotificationService() {
        // Qui inizializzi la connessione a RabbitMQ
        System.out.println("Inizializzazione Notification Service su RabbitMQ...");
    }

    private static void startSearchService() {
        // Qui inizializzi la connessione a Elasticsearch
        System.out.println("Inizializzazione Search Service su Elasticsearch...");
    }

    private static void startDocumentService() {
        // Qui avvii il microservizio Document Service
        System.out.println("Inizializzazione Document Service...");
        DocumentServiceApplication.start();
    }
}
