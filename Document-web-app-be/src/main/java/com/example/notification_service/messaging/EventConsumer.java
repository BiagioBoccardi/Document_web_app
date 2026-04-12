package com.example.notification_service.messaging;

import java.nio.charset.StandardCharsets;

import com.example.notification_service.config.MetricsManager;
import com.example.notification_service.service.NotificationService;
import com.example.notification_service.service.NotificationTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class EventConsumer {
    private final NotificationService notificationService;
    private final NotificationTemplateService templateService;
    private final MetricsManager metricsManager;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String QUEUE_NAME = "notification_queue";
    
    private Connection connection;
    private Channel channel;

    public void startListening() {
        int attempts = 0;
        int maxAttempts = 5;

        while (attempts < maxAttempts) {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                // L'host deve essere "rabbitmq" come nel docker-compose
                factory.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "localhost"));
                
                this.connection = factory.newConnection();
                this.channel = connection.createChannel();

                channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    long startTime = System.currentTimeMillis();

                    try {
                        GenericEvent event = objectMapper.readValue(message, GenericEvent.class);

                        // Calcolo della latenza end-to-end
                        if (event.getMetadata() != null && event.getMetadata().containsKey("published_at")) {
                            long publishedAt = ((Number) event.getMetadata().get("published_at")).longValue();
                            long latency = startTime - publishedAt;
                            
                            // Logga la latenza in InfluxDB
                            metricsManager.logEvent("notification_latency", "event_type", event.getType(), "ms", latency);
                        }

                        processEvent(event);
                        metricsManager.logEvent("notification_status", "status", "success", "count", 1);
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

                    } catch (Exception e) {
                        log.error("Errore elaborazione evento: {}", message, e);
                        metricsManager.logEvent("notification_status", "status", "failure", "count", 1);
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                    }
                };

                channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
                
                log.info("Ricezione eventi attiva sulla coda: {}", QUEUE_NAME);
                break; 

            } catch (Exception e) {
                attempts++;
                log.warn("Tentativo {}/{} fallito: RabbitMQ non raggiungibile. Riprovo tra 5 secondi...", attempts, maxAttempts);
                
                if (attempts >= maxAttempts) {
                    log.error("Massimo numero di tentativi raggiunto. Notifiche via eventi DISABILITATE.");
                } else {
                    try {
                        Thread.sleep(5000); 
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private void processEvent(GenericEvent event) {
        log.info("Processing evento: {}", event.getType());
        
        // Il TemplateService genera già il messaggio completo usando i metadata
        String message = templateService.generateMessage(event.getType(), event.getMetadata());

        if (message != null) {
            notificationService.createNotification(event.getUserId(), message);
            log.debug("Notifica persistita per l'evento {}", event.getType());
        }
    }

    public void stopListening() {
        log.info("Chiusura connessioni RabbitMQ...");
        try {
            if (channel != null && channel.isOpen()) channel.close();
            if (connection != null && connection.isOpen()) connection.close();
        } catch (Exception e) {
            log.error("Errore durante la chiusura di RabbitMQ", e);
        }
    }
}