package com.example.user_service.repository;

import com.example.user_service.model.Gruppo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import java.util.List;
import java.util.Optional;

public class GruppoRepository {

    private final EntityManagerFactory entityManagerFactory;

    public GruppoRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public Gruppo save(Gruppo gruppo) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            try {
                entityManager.getTransaction().begin();
                entityManager.persist(gruppo);
                entityManager.getTransaction().commit();
                return gruppo;
            } catch (RuntimeException ex) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw ex;
            }
        }
    }

    public Optional<Gruppo> findById(int id) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            Gruppo result = entityManager.find(Gruppo.class, id);
            return Optional.ofNullable(result);
        }
    }

    public Optional<Gruppo> findByIdWithDetails(int id) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            try {
                Gruppo result = entityManager
                        .createQuery("SELECT g FROM Gruppo g LEFT JOIN FETCH g.owner LEFT JOIN FETCH g.membri WHERE g.id = :id", Gruppo.class)
                        .setParameter("id", id)
                        .getSingleResult();
                return Optional.of(result);
            } catch (NoResultException ex) {
                return Optional.empty();
            }
        }
    }

    public List<Gruppo> findAll() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            return entityManager.createQuery("from Gruppo", Gruppo.class).getResultList();
        }
    }

    public List<Gruppo> findAllWithDetails() {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            return entityManager.createQuery("SELECT DISTINCT g FROM Gruppo g LEFT JOIN FETCH g.owner LEFT JOIN FETCH g.membri", Gruppo.class).getResultList();
        }
    }

    public Gruppo update(Gruppo gruppo) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            try {
                entityManager.getTransaction().begin();
                Gruppo merged = entityManager.merge(gruppo);
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

    public void delete(int id) {
        try (EntityManager entityManager = entityManagerFactory.createEntityManager()) {
            try {
                entityManager.getTransaction().begin();
                Gruppo gruppo = entityManager.find(Gruppo.class, id);
                if (gruppo != null) {
                    entityManager.remove(gruppo);
                }
                entityManager.getTransaction().commit();
            } catch (RuntimeException ex) {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                throw ex;
            }
        }
    }
}
