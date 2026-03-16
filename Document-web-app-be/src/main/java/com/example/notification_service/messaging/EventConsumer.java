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
    

    public void startListening() {
        // Se RabbitMQ non è disponibile, puoi loggare uno stub e saltare la connessione
        try {
            ConnectionFactory factory = new ConnectionFactory();
            // Carica configurazione via ENV
            factory.setHost(System.getenv().getOrDefault("RABBITMQ_HOST", "localhost"));
            
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // Dichiarazione coda e binding (semplificato)
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            log.info("In attesa di eventi sulla coda: {}", QUEUE_NAME);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                try {
                    GenericEvent event = objectMapper.readValue(message, GenericEvent.class);
                    processEvent(event);
                    // Conferma ricezione (Ack) per garantire resilienza
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (Exception e) {
                    log.error("Errore nel processing dell'evento: {}", message, e);
                    // In caso di errore, il messaggio può finire in una DLQ o essere ritentato
                    channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                }
            };

            channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});

        } catch (Exception e) {
            log.warn("Impossibile connettersi a RabbitMQ. Il servizio funzionerà solo via API. Errore: {}", e.getMessage());
        }
    }

    private void processEvent(GenericEvent event) {
        log.info("Ricevuto evento di tipo: {}", event.getType());
        String message = templateService.generateMessage(event.getType(), event.getMetadata());

        switch (event.getType()) {
            case "user.registered":
                notificationService.createNotification(event.getUserId(), message); 
                break;
                
            case "document.uploaded":
                String fileName = (String) event.getMetadata().getOrDefault("fileName", "documento");
                notificationService.createNotification(event.getUserId(), message + fileName); 
                break;
                
            case "search.completed":
                notificationService.createNotification(event.getUserId(), message); 
                break;

            default:
                log.warn("Tipo evento sconosciuto: {}", event.getType());
        }
    }

    public void stopListening() {
        // Stub implementation for stopping RabbitMQ event consumption
    }
}