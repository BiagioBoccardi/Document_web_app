package com.example.user_service.repository;

import java.util.Optional;

import com.example.user_service.model.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

public class UserRepository {

    private final EntityManagerFactory entityManagerFactory;

    public UserRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public User save(User utente) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            try {
                entityManager.getTransaction().begin();
                entityManager.persist(utente);
                entityManager.getTransaction().commit();
                return utente;
            } catch (RuntimeException ex) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw ex;
            }
        }
    }

    public Optional<User> findByEmail(String email) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            try {
                User result = entityManager
                        .createQuery("SELECT u FROM UtenteModel u WHERE u.email = :email", User.class)
                        .setParameter("email", email)
                        .getSingleResult();
                return Optional.of(result);
            } catch (NoResultException ex) {
                return Optional.empty();
            }
        }
    }

    public Optional<User> findById(int id) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            User result = entityManager.find(User.class, id);
            return Optional.ofNullable(result);
        }
    }

    public User update(User utente) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            try {
                entityManager.getTransaction().begin();
                User merged = entityManager.merge(utente);
                entityManager.getTransaction().commit();
                return merged;
            } catch (RuntimeException ex) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw ex;
            }
        }
    }
}