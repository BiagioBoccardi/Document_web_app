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
        return gruppoRepository.save(gruppo);
    }

    public Optional<Gruppo> getGruppoById(Long id) {
        return gruppoRepository.findByIdWithDetails(id);
    }

    public List<Gruppo> getAllGruppi() {
        return gruppoRepository.findAllWithDetails();
    }

    public void deleteGruppo(Long id) {
        gruppoRepository.delete(id);
    }

    public void addMembro(Long gruppoId, User user) {
        Optional<Gruppo> gruppoOpt = gruppoRepository.findByIdWithDetails(gruppoId);
        if (gruppoOpt.isPresent()) {
            Gruppo gruppo = gruppoOpt.get();
            gruppo.getMembri().add(user);
            gruppoRepository.update(gruppo);
        }
    }
}
