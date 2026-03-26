package com.example.user_service.service;

import java.util.List;
import java.util.Optional;

import com.example.user_service.model.Gruppo;
import com.example.user_service.model.User;
import com.example.user_service.repository.GruppoRepository;
import com.example.user_service.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GruppoService {

    private final GruppoRepository gruppoRepository;
    private final UserRepository userRepository;


    public Gruppo createGruppo(String name, int ownerId, List<Integer> members) {

        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new IllegalArgumentException("Owner non trovato"));

        Gruppo gruppo = new Gruppo();
        gruppo.setName(name);
        gruppo.setOwner(owner);

        gruppo.getMembers().add(owner);

        if (members != null) {
            for (Integer id : members) {
                if (id == ownerId) continue; 

                userRepository.findById(id).ifPresent(member -> {
                    if (!gruppo.getMembers().contains(member)) {
                        gruppo.getMembers().add(member);
                    }
                });
            }
        }
        return gruppoRepository.save(gruppo);
    }

    public Optional<Gruppo> getGruppoById(int id) {
        return gruppoRepository.findByIdWithDetails(id);
    }

    public List<Gruppo> getAllGruppi() {
        return gruppoRepository.findAllWithDetails();
    }

    public void deleteGruppo(int id) {
        gruppoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Gruppo non trovato"));
        gruppoRepository.delete(id);
    }

    public Gruppo addMembro(int gruppoId, int userId) {
        Gruppo gruppo = gruppoRepository.findByIdWithDetails(gruppoId)
                .orElseThrow(() -> new IllegalArgumentException("Gruppo non trovato"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utente non trovato"));

        boolean giaPresente = gruppo.getMembers().stream().anyMatch(m -> m.getId() == userId);
        if (giaPresente) {
            throw new IllegalStateException("Utente già membro del gruppo");
        }

        gruppo.getMembers().add(user);
        return gruppoRepository.update(gruppo);
    }

    public Gruppo removeMembro(int gruppoId, int userId) {   
        Gruppo gruppo = gruppoRepository.findByIdWithDetails(gruppoId)
                .orElseThrow(() -> new IllegalArgumentException("Gruppo non trovato"));

        // Non si può rimuovere l'owner
        if (gruppo.getOwner().getId() == userId) {
            throw new IllegalStateException("Non puoi rimuovere il proprietario dal gruppo");
        }

        gruppo.getMembers().removeIf(m -> m.getId() == userId);
        return gruppoRepository.update(gruppo);
    }
}