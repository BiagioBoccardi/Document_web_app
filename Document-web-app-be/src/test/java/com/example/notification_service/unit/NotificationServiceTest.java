package com.example.notification_service.unit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.notification_service.entity.Notification;
import com.example.notification_service.repository.NotificationRepository;
import com.example.notification_service.service.NotificationService;

public class NotificationServiceTest {
    private final NotificationRepository mockRepo = mock(NotificationRepository.class);
    private final NotificationService service = new NotificationService(mockRepo);

    @Test
    void testMarkAsReadSuccess() {
        UUID notificationId = UUID.randomUUID();
        int userId = 1;
        
        Notification notification = new Notification();
        notification.setUuid(notificationId);
        notification.setUserId(userId);
        notification.setStato("SENT");

        when(mockRepo.findByIdAndUser(notificationId, userId)).thenReturn(Optional.of(notification));

        service.markAsRead(notificationId, userId);

        assertEquals("READ", notification.getStato()); //
        verify(mockRepo, times(1)).update(notification);
    }

    @Test
    void testMarkAsReadFailsWhenNotOwner() {
        UUID notificationId = UUID.randomUUID();
        // Se il repo non trova nulla (perché l'ID o l'utente sono sbagliati)
        when(mockRepo.findByIdAndUser(notificationId, 999)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.markAsRead(notificationId, 999));
    }

    @Test
    @DisplayName("Recupero notifiche tramite Service")
    void testGetNotificationsForUser() {
       int userId = 1;
        String status = "SENT";
        int page = 1;
        int size = 10;
        int expectedOffset = 0;

        when(mockRepo.findByUserId(userId, status, expectedOffset, size))
            .thenReturn(List.of(new Notification(), new Notification()));

        List<Notification> result = service.getUserNotifications(userId, status, page, size);

        assertEquals(2, result.size());
        verify(mockRepo, times(1)).findByUserId(userId, status, expectedOffset, size);
    }

    @Test
    @DisplayName("Creazione nuova notifica con stato iniziale")
    void testCreateNotification() {
        int userId = 1;
        String messaggio = "Messaggio di prova";

        Notification result = service.createNotification(userId, messaggio);

        assertNotNull(result);
        assertEquals("SENT", result.getStato());
        // Verifichiamo che il metodo setMessaggio (nome corretto nel tuo codice) sia stato usato
        assertEquals(messaggio, result.getMessaggio()); 
        
        verify(mockRepo, times(1)).save(any(Notification.class));
    }
}
