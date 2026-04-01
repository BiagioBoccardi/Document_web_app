package com.example.notification_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import com.example.notification_service.config.HibernateUtil;
import com.example.notification_service.entity.Notification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotificationRepository {
    // Salva una nuova notifica
    public void save(Notification notification) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.persist(notification);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            log.error("Errore durante il salvataggio della notifica", e);
        }
    }

    // Lista notifiche per utente con filtri
    public List<Notification> findByUserId(int userId, String status, int offset, int limit) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "FROM Notification n WHERE n.userId = :userId";
            if (status != null) {
                hql += " AND n.stato = :status";
            }
            hql += " ORDER BY n.createdAt DESC";

            Query<Notification> query = session.createQuery(hql, Notification.class);
            query.setParameter("userId", userId);
            if (status != null) query.setParameter("status", status);
            
            query.setFirstResult(offset);
            query.setMaxResults(limit);
            
            return query.list();
        }
    }

    // Trova una specifica notifica verificando il proprietario 
    public Optional<Notification> findByIdAndUser(UUID uuid, int userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                "FROM Notification n WHERE n.uuid = :uuid AND n.userId = :userId", Notification.class)
                .setParameter("uuid", uuid)
                .setParameter("userId", userId)
                .uniqueResultOptional();
        }
    }

    // Aggiorna lo stato della notifica 
    public void update(Notification notification) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(notification);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            log.error("Errore durante l'aggiornamento della notifica", e);
        }
    }

    // Elimina una notifica
    public void delete(Notification notification) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.remove(notification);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) transaction.rollback();
            log.error("Errore durante l'eliminazione della notifica", e);
        }
    }
}
