package com.example.notification_service.messaging;

import java.nio.charset.StandardCharsets;

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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String QUEUE_NAME = "notification_queue";
    
    private Connection connection;
    private Channel channel;

    public void startListening() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "localhost"));
            
            this.connection = factory.newConnection();
            this.channel = connection.createChannel();

            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            log.info("Ricezione eventi attiva sulla coda: {}", QUEUE_NAME);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                try {
                    GenericEvent event = objectMapper.readValue(message, GenericEvent.class);
                    processEvent(event);
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (Exception e) {
                    log.error("Errore elaborazione evento: {}", message, e);
                    // In caso di errore, non confermiamo (requeue = true per riprovare)
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                }
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

        } catch (Exception e) {
            log.warn("RabbitMQ non raggiungibile. Notifiche via eventi disabilitate: {}", e.getMessage());
        }
    }

    private void processEvent(GenericEvent event) {
        log.info("Processing evento: {}", event.getType());
        
        // Il TemplateService genera già il messaggio completo usando i metadata
        String message = templateService.generateMessage(event.getType(), event.getMetadata());

        // Ora la logica è centralizzata: il service crea la notifica indipendentemente dal tipo
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