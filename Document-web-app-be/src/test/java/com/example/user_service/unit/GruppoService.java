package com.example.user_service.unit;

import com.example.user_service.model.Gruppo;
import com.example.user_service.model.User;
import com.example.user_service.repository.GruppoRepository;
import com.example.user_service.service.GruppoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("GruppoService - Test unitari")
class GruppoServiceTest {

    @Mock
    private GruppoRepository gruppoRepository;

    @InjectMocks
    private GruppoService gruppoService;

    // ─────────────────────────────────────────────
    // Helper: costruisce User e Gruppo di supporto
    // ─────────────────────────────────────────────
    private User buildUser(int id, String nome, String email) {
        User u = new User();
        u.setId(id);
        u.setNome(nome);
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setAdmin(false);
        return u;
    }

    private Gruppo buildGruppo(int id, String nome, User owner) {
        Gruppo g = new Gruppo();
        g.setId(id);
        g.setName(nome);
        g.setOwner(owner);
        // membri deve essere mutabile per addMembro/removeMembro
        g.setMembri(new ArrayList<>(List.of(owner)));
        return g;
    }

    // ═════════════════════════════════════════════
    // createGruppo()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("createGruppo()")
    class CreateGruppo {

        @Test
        @DisplayName("OK: crea gruppo con owner e lo aggiunge ai membri")
        void shouldCreateGruppoWithOwnerAsMember() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            when(gruppoRepository.save(any(Gruppo.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            Gruppo result = gruppoService.createGruppo("Team Alpha", owner);

            assertAll(
                    () -> assertEquals("Team Alpha", result.getName()),
                    () -> assertEquals(owner, result.getOwner()),
                    () -> assertTrue(result.getMembri().contains(owner),
                            "L'owner deve essere automaticamente aggiunto ai membri")
            );
            verify(gruppoRepository, times(1)).save(any(Gruppo.class));
        }
    }

    // ═════════════════════════════════════════════
    // getGruppoById()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("getGruppoById()")
    class GetGruppoById {

        @Test
        @DisplayName("OK: restituisce Optional con gruppo esistente")
        void shouldReturnGruppoById() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            Gruppo gruppo = buildGruppo(1, "Team Alpha", owner);
            when(gruppoRepository.findByIdWithDetails(1))
                    .thenReturn(Optional.of(gruppo));

            Optional<Gruppo> result = gruppoService.getGruppoById(1);

            assertTrue(result.isPresent());
            assertEquals("Team Alpha", result.get().getName());
        }

        @Test
        @DisplayName("OK: restituisce Optional vuoto per id inesistente")
        void shouldReturnEmptyWhenNotFound() {
            when(gruppoRepository.findByIdWithDetails(99))
                    .thenReturn(Optional.empty());

            Optional<Gruppo> result = gruppoService.getGruppoById(99);

            assertTrue(result.isEmpty());
        }
    }

    // ═════════════════════════════════════════════
    // getAllGruppi()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("getAllGruppi()")
    class GetAllGruppi {

        @Test
        @DisplayName("OK: restituisce lista completa di gruppi")
        void shouldReturnAllGruppi() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            List<Gruppo> gruppi = List.of(
                    buildGruppo(1, "Team Alpha", owner),
                    buildGruppo(2, "Team Beta", owner)
            );
            when(gruppoRepository.findAllWithDetails()).thenReturn(gruppi);

            List<Gruppo> result = gruppoService.getAllGruppi();

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("OK: restituisce lista vuota se non ci sono gruppi")
        void shouldReturnEmptyList() {
            when(gruppoRepository.findAllWithDetails()).thenReturn(List.of());

            List<Gruppo> result = gruppoService.getAllGruppi();

            assertTrue(result.isEmpty());
        }
    }

    // ═════════════════════════════════════════════
    // deleteGruppo()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("deleteGruppo()")
    class DeleteGruppo {

        @Test
        @DisplayName("OK: elimina gruppo esistente")
        void shouldDeleteExistingGruppo() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            Gruppo gruppo = buildGruppo(1, "Team Alpha", owner);
            when(gruppoRepository.findById(1)).thenReturn(Optional.of(gruppo));

            assertDoesNotThrow(() -> gruppoService.deleteGruppo(1));

            verify(gruppoRepository, times(1)).delete(1);
        }

        @Test
        @DisplayName("KO: id inesistente lancia IllegalArgumentException")
        void shouldThrowWhenGruppoNotFound() {
            when(gruppoRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> gruppoService.deleteGruppo(99));

            // delete NON deve essere chiamato
            verify(gruppoRepository, never()).delete(anyInt());
        }
    }

    // ═════════════════════════════════════════════
    // addMembro()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("addMembro()")
    class AddMembro {

        @Test
        @DisplayName("OK: aggiunge nuovo membro al gruppo")
        void shouldAddNewMembro() {
            User owner  = buildUser(1, "Mario", "mario@example.com");
            User newMember = buildUser(2, "Luigi", "luigi@example.com");
            Gruppo gruppo = buildGruppo(1, "Team Alpha", owner);

            when(gruppoRepository.findByIdWithDetails(1)).thenReturn(Optional.of(gruppo));
            when(gruppoRepository.update(any(Gruppo.class))).thenAnswer(inv -> inv.getArgument(0));

            Gruppo result = gruppoService.addMembro(1, newMember);

            assertTrue(result.getMembri().contains(newMember));
            verify(gruppoRepository, times(1)).update(any(Gruppo.class));
        }

        @Test
        @DisplayName("KO: utente già membro lancia IllegalStateException")
        void shouldThrowWhenAlreadyMember() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            Gruppo gruppo = buildGruppo(1, "Team Alpha", owner);

            when(gruppoRepository.findByIdWithDetails(1)).thenReturn(Optional.of(gruppo));

            // owner è già membro
            assertThrows(IllegalStateException.class,
                    () -> gruppoService.addMembro(1, owner));

            verify(gruppoRepository, never()).update(any());
        }

        @Test
        @DisplayName("KO: gruppo inesistente lancia IllegalArgumentException")
        void shouldThrowWhenGruppoNotFound() {
            User user = buildUser(2, "Luigi", "luigi@example.com");
            when(gruppoRepository.findByIdWithDetails(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> gruppoService.addMembro(99, user));
        }
    }

    // ═════════════════════════════════════════════
    // removeMembro()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("removeMembro()")
    class RemoveMembro {

        @Test
        @DisplayName("OK: rimuove membro dal gruppo")
        void shouldRemoveMembro() {
            User owner  = buildUser(1, "Mario", "mario@example.com");
            User member = buildUser(2, "Luigi", "luigi@example.com");
            Gruppo gruppo = buildGruppo(1, "Team Alpha", owner);
            gruppo.getMembri().add(member);

            when(gruppoRepository.findByIdWithDetails(1)).thenReturn(Optional.of(gruppo));
            when(gruppoRepository.update(any(Gruppo.class))).thenAnswer(inv -> inv.getArgument(0));

            Gruppo result = gruppoService.removeMembro(1, member);

            assertFalse(result.getMembri().stream()
                    .anyMatch(m -> m.getId() == member.getId()));
            verify(gruppoRepository, times(1)).update(any(Gruppo.class));
        }

        @Test
        @DisplayName("KO: non si può rimuovere l'owner lancia IllegalStateException")
        void shouldThrowWhenRemovingOwner() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            Gruppo gruppo = buildGruppo(1, "Team Alpha", owner);

            when(gruppoRepository.findByIdWithDetails(1)).thenReturn(Optional.of(gruppo));

            assertThrows(IllegalStateException.class,
                    () -> gruppoService.removeMembro(1, owner));

            verify(gruppoRepository, never()).update(any());
        }

        @Test
        @DisplayName("KO: gruppo inesistente lancia IllegalArgumentException")
        void shouldThrowWhenGruppoNotFound() {
            User user = buildUser(2, "Luigi", "luigi@example.com");
            when(gruppoRepository.findByIdWithDetails(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> gruppoService.removeMembro(99, user));
        }
    }
}