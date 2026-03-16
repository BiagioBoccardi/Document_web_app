package com.example.notification_service.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            Configuration configuration = new Configuration().configure("notification.hibernate.cfg.xml");

            // Recuperiamo i valori con un fallback di default
            String user = System.getenv().getOrDefault("POSTGRES_USER", "postgres");
            String pass = System.getenv().getOrDefault("POSTGRES_PASSWORD", "postgres");
            
            // Se sei in locale (localhost), assicurati che l'URL punti a localhost:5432 nell'XML
            configuration.setProperty("hibernate.connection.username", user);
            configuration.setProperty("hibernate.connection.password", pass);

            return configuration.buildSessionFactory();
        } catch (Exception ex) {
            log.error("Inizializzazione SessionFactory fallita: ", ex);
            // NON lanciare ExceptionInInitializerError qui se vuoi che il test 
            // possa almeno caricare la classe per farne il mock
            return null; 
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
