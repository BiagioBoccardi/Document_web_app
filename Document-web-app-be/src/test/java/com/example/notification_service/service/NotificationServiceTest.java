package com.example.notification_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.notification_service.entity.Notification;
import com.example.notification_service.repository.NotificationRepository;

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

        // Simula il database che restituisce la notifica
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
}
