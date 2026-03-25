package com.example.user_service.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // Puntiamo al file XML specifico per lo User Service
            Configuration configuration = new Configuration()
                    .configure("META-INF/user.hibernate.cfg.xml");

            // Recupero credenziali dalle variabili d'ambiente (comuni o specifiche)
            String dbHost = System.getenv().getOrDefault("DB_HOST", "localhost");
            String user = System.getenv().getOrDefault("POSTGRES_USER", "postgres");
            String pass = System.getenv().getOrDefault("POSTGRES_PASSWORD", "Bonelle");

            String dbUrl = "jdbc:postgresql://" + dbHost + ":5432/document_web_app";
            
            configuration.setProperty("hibernate.connection.url", dbUrl);
            configuration.setProperty("hibernate.connection.username", user);
            configuration.setProperty("hibernate.connection.password", pass);

            log.info("SessionFactory di User-Service inizializzata correttamente.");
            return configuration.buildSessionFactory();
        } catch (Exception ex) {
            log.error("Inizializzazione SessionFactory (User-Service) fallita: ", ex);
            return null; 
        }
    }

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            throw new IllegalStateException("La SessionFactory non è stata inizializzata correttamente.");
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            log.info("Chiusura SessionFactory di User-Service...");
            sessionFactory.close();
        }
    }
}
