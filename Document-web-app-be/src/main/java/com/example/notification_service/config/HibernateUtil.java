package com.example.notification_service.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HibernateUtil {
    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // Carica la configurazione specifica per le notifiche
            return new Configuration()
                    .configure("notification.hibernate.cfg.xml") 
                    .buildSessionFactory();
        } catch (Exception ex) {
            log.error("Inizializzazione SessionFactory fallita: ", ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        getSessionFactory().close();
    }
}
