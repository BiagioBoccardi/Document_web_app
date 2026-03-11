package com.example.user_service.service;

import com.example.user_service.model.Gruppo;
import com.example.user_service.model.User;
import com.example.user_service.repository.GruppoRepository;

import java.util.List;
import java.util.Optional;

public class GruppoService {

    private final GruppoRepository gruppoRepository;

    public GruppoService(GruppoRepository gruppoRepository) {
        this.gruppoRepository = gruppoRepository;
    }

    public Gruppo createGruppo(String name, User owner) {
        Gruppo gruppo = new Gruppo();
        gruppo.setName(name);
        gruppo.setOwner(owner);
        gruppo.getMembri().add(owner);      // ← owner aggiunto come membro
        return gruppoRepository.save(gruppo);
    }

    public Optional<Gruppo> getGruppoById(int id) {
        return gruppoRepository.findByIdWithDetails(id);
    }

    public List<Gruppo> getAllGruppi() {
        return gruppoRepository.findAllWithDetails();
    }

    public void deleteGruppo(int id) {
        // ← verifica esistenza prima di eliminare
        gruppoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gruppo non trovato"));
        gruppoRepository.delete(id);
    }

    public Gruppo addMembro(int gruppoId, User user) {
        Gruppo gruppo = gruppoRepository.findByIdWithDetails(gruppoId)
                .orElseThrow(() -> new IllegalArgumentException("Gruppo non trovato"));

        // ← controllo duplicati
        boolean giaPresente = gruppo.getMembri().stream()
                .anyMatch(m -> m.getId() == user.getId());
        if (giaPresente) {
            throw new IllegalStateException("Utente già membro del gruppo");
        }

        gruppo.getMembri().add(user);
        return gruppoRepository.update(gruppo);
    }

    public Gruppo removeMembro(int gruppoId, User user) {   // ← metodo aggiunto
        Gruppo gruppo = gruppoRepository.findByIdWithDetails(gruppoId)
                .orElseThrow(() -> new IllegalArgumentException("Gruppo non trovato"));

        // Non si può rimuovere l'owner
        if (gruppo.getOwner().getId() == user.getId()) {
            throw new IllegalStateException("Non puoi rimuovere il proprietario dal gruppo");
        }

        gruppo.getMembri().removeIf(m -> m.getId() == user.getId());
        return gruppoRepository.update(gruppo);
    }
}