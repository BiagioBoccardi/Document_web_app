package com.example.search_service.config;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory per la creazione e gestione della connessione RabbitMQ.
 */
@Slf4j
public class RabbitMQConfig {

    private static Connection connection;

    // Nomi delle code consumate dal Search Service
    public static final String QUEUE_DOCUMENT_UPLOADED = "document.uploaded";
    public static final String QUEUE_DOCUMENT_UPDATED  = "document.updated";
    public static final String QUEUE_DOCUMENT_DELETED  = "document.deleted";

    public static Connection createConnection() {
        String host     = DotenvConfig.get("RABBITMQ_HOST", "localhost");
        int    port     = DotenvConfig.getAsInt("RABBITMQ_PORT", 5672);
        String username = DotenvConfig.get("RABBITMQ_USERNAME", "guest");
        String password = DotenvConfig.get("RABBITMQ_PASSWORD", "guest");
        String vhost    = DotenvConfig.get("RABBITMQ_VHOST", "/");

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost(vhost);

            // Riconnessione automatica
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(5000); // riprova ogni 5 secondi

            connection = factory.newConnection();
            log.info("Connessione RabbitMQ stabilita su {}:{}", host, port);
            return connection;

        } catch (Exception e) {
            log.error("Impossibile connettersi a RabbitMQ su {}:{} — {}", host, port, e.getMessage());
            throw new RuntimeException("Connessione RabbitMQ fallita", e);
        }
    }

    public static void closeConnection() {
        if (connection != null && connection.isOpen()) {
            try {
                connection.close();
                log.info("Connessione RabbitMQ chiusa");
            } catch (Exception e) {
                log.warn("Errore durante la chiusura della connessione RabbitMQ: {}", e.getMessage());
            }
        }
    }
}
