package com.example.notification_service.service;

import com.example.notification_service.entity.Notification;
import com.example.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Recupera le notifiche di un utente specifico con paginazione e filtri.
     */
    public List<Notification> getUserNotifications(int userId, String status, int page, int size) {
        log.info("Recupero notifiche per utente: {} (Stato: {}, Pagina: {})", userId, status, page);
        int offset = (page - 1) * size;
        return repository.findByUserId(userId, status, offset, size);
    }

    /**
     * Crea e persiste una nuova notifica. Metodo usato dai consumer degli eventi.
     */
    public Notification createNotification(int userId, String messaggio) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessaggio(messaggio);
        notification.setStato("SENT"); // Stato iniziale standard
        
        repository.save(notification);
        log.debug("Notifica creata con successo per l'utente {}", userId);
        return notification;
    }

    /**
     * Marca una notifica come letta, verificando che appartenga all'utente.
     */
    public void markAsRead(UUID notificationId, int userId) {
        Notification notification = repository.findByIdAndUser(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Notifica non trovata o accesso negato"));

        if (!"READ".equals(notification.getStato())) {
            notification.setStato("READ");
            notification.setReadAt(LocalDateTime.now());
            repository.update(notification);
            log.info("Notifica {} marcata come letta", notificationId);
        }
    }

    /**
     * Elimina una notifica specifica previa verifica di proprietà.
     */
    public void deleteNotification(UUID notificationId, int userId) {
        Notification notification = repository.findByIdAndUser(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Impossibile eliminare: notifica non trovata"));

        repository.delete(notification);
        log.info("Notifica {} eliminata dall'utente {}", notificationId, userId);
    }
}