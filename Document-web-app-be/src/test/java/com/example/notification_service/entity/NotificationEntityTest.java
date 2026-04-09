package com.example.notification_service.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NotificationEntityTest {
    @Test
    @DisplayName("Test: Copertura totale Entity (Getter, Setter, Builder, Costruttori)")
    void testNotificationEntityFull() {
        UUID id = UUID.randomUUID();
        LocalDateTime ora = LocalDateTime.now();

        
        Notification n = Notification.builder()
                .uuid(id)
                .messaggio("Test")
                .userId(1)
                .stato("SENT")
                .createdAt(ora)
                .readAt(ora)
                .build();

        
        assertEquals(id, n.getUuid());
        assertEquals("Test", n.getMessaggio());
        assertEquals(1, n.getUserId());
        assertEquals("SENT", n.getStato());
        assertEquals(ora, n.getCreatedAt());
        assertEquals(ora, n.getReadAt());

        
        Notification empty = new Notification();
        empty.setMessaggio("Nuovo");
        assertEquals("Nuovo", empty.getMessaggio());
    }

    @Test
    @DisplayName("Test: Metodo @PrePersist onCreate")
    void testOnCreateLifecycle() {
        Notification n = new Notification();
        n.setStato(null); 

        n.onCreate(); 

        assertNotNull(n.getCreatedAt(), "createdAt dovrebbe essere impostato");
        assertEquals("PENDING", n.getStato(), "Lo stato nullo dovrebbe diventare PENDING");

        Notification n2 = new Notification();
        n2.setStato("SENT");
        n2.onCreate();
        assertEquals("SENT", n2.getStato(), "Lo stato non dovrebbe cambiare se già presente");
    }

    @Test
    @DisplayName("Test: Metodi Equals, HashCode e ToString")
    void testLombokMethods() {
        Notification n1 = Notification.builder().uuid(UUID.randomUUID()).build();
        Notification n2 = Notification.builder().uuid(n1.getUuid()).build();

        assertEquals(n1, n2);
        assertEquals(n1.hashCode(), n2.hashCode());
        assertNotNull(n1.toString());
    }
}
