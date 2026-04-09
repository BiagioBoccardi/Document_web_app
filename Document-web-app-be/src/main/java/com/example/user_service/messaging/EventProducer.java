package com.example.user_service.messaging;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventProducer {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String QUEUE_NAME = "notification_queue";
    private final String host;

    public EventProducer() {
        this.host = System.getenv().getOrDefault("RABBITMQ_HOST", "localhost");
    }

    public void sendEvent(String type, int userId, Map<String, Object> metadata) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            
            // Dichiarazione della coda (deve corrispondere a quella del consumer)
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            // Creiamo il payload (GenericEvent)
            Map<String, Object> event = Map.of(
                "type", type,
                "user_id", userId, // Usiamo snake_case per Jackson
                "metadata", metadata != null ? metadata : Map.of()
            );

            String message = objectMapper.writeValueAsString(event);
            
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            log.info("Evento [ {} ] inviato alla coda per l'utente {}", type, userId);

        } catch (Exception e) {
            log.error("Errore durante l'invio dell'evento a RabbitMQ", e);
        }
    }
}
