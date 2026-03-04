<<<<<<< HEAD
package com.example.user_service.repository;

import java.util.Optional;

import com.example.user_service.model.UtenteModel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

public class UserRepository {

    private final EntityManagerFactory entityManagerFactory;

    public UserRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public UtenteModel save(UtenteModel utente) {
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

    public Optional<UtenteModel> findByEmail(String email) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            try {
                UtenteModel result = entityManager
                        .createQuery("SELECT u FROM UtenteModel u WHERE u.email = :email", UtenteModel.class)
                        .setParameter("email", email)
                        .getSingleResult();
                return Optional.of(result);
            } catch (NoResultException ex) {
                return Optional.empty();
            }
        }
    }

    public Optional<UtenteModel> findById(int id) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            UtenteModel result = entityManager.find(UtenteModel.class, id);
            return Optional.ofNullable(result);
        }
    }

    public UtenteModel update(UtenteModel utente) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            try {
                entityManager.getTransaction().begin();
                UtenteModel merged = entityManager.merge(utente);
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
=======
package com.example.user_service.repository;


public class UserRepository {
    
}
>>>>>>> f6285c2b857ee000d17288c2e1fdd1fe77991e43
