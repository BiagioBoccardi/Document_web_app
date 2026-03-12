package com.example.user_service.integration;

import com.example.user_service.controller.GruppoController;
import com.example.user_service.dto.CreateGruppoRequest;
import com.example.user_service.dto.MembroRequest;
import com.example.user_service.model.Gruppo;
import com.example.user_service.model.User;
import com.example.user_service.service.GruppoService;
import com.example.user_service.service.UserService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GruppoController - Test unitari")
class GruppoControllerTest {

    @Mock
    private GruppoService gruppoService;

    @Mock
    private UserService userService;

    @Mock
    private Context ctx;

    @InjectMocks
    private GruppoController gruppoController;

    @BeforeEach
    void setupCtxChain() {
        // ctx.status(...) restituisce ctx per supportare la catena .status().json()
        lenient().when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);
    }

    // ─────────────────────────────────────────────
    // Helper
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
        g.setMembri(new ArrayList<>(List.of(owner)));
        return g;
    }

    private CreateGruppoRequest buildCreateRequest(String name, int ownerId) {
        CreateGruppoRequest req = new CreateGruppoRequest();
        req.name = name;
        req.ownerId = ownerId;
        return req;
    }

    private MembroRequest buildMembroRequest(int userId) {
        MembroRequest req = new MembroRequest();
        req.userId = userId;
        return req;
    }

    // Invoca un metodo privato del controller tramite reflection
    private void invoke(String methodName) {
        try {
            var method = GruppoController.class.getDeclaredMethod(methodName, Context.class);
            method.setAccessible(true);
            method.invoke(gruppoController, ctx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ═════════════════════════════════════════════
    // POST /api/gruppi  →  createGruppo()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/gruppi")
    class CreateGruppo {

        @Test
        @DisplayName("201 CREATED: gruppo creato con successo")
        void shouldReturn201WhenGruppoIsCreated() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            Gruppo gruppo = buildGruppo(1, "Team Alpha", owner);
            CreateGruppoRequest req = buildCreateRequest("Team Alpha", 1);

            when(ctx.bodyAsClass(CreateGruppoRequest.class)).thenReturn(req);
            when(userService.getProfile(1)).thenReturn(owner);
            when(gruppoService.createGruppo("Team Alpha", owner)).thenReturn(gruppo);

            invoke("createGruppo");

            verify(ctx).status(HttpStatus.CREATED);
            verify(ctx).json(gruppo);
        }

        @Test
        @DisplayName("400 BAD REQUEST: nome mancante")
        void shouldReturn400WhenNameIsNull() {
            CreateGruppoRequest req = buildCreateRequest(null, 1);
            when(ctx.bodyAsClass(CreateGruppoRequest.class)).thenReturn(req);

            invoke("createGruppo");

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(gruppoService, never()).createGruppo(any(), any());
        }

        @Test
        @DisplayName("400 BAD REQUEST: nome blank")
        void shouldReturn400WhenNameIsBlank() {
            CreateGruppoRequest req = buildCreateRequest("   ", 1);
            when(ctx.bodyAsClass(CreateGruppoRequest.class)).thenReturn(req);

            invoke("createGruppo");

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(gruppoService, never()).createGruppo(any(), any());
        }

        @Test
        @DisplayName("404 NOT FOUND: owner inesistente")
        void shouldReturn404WhenOwnerNotFound() {
            CreateGruppoRequest req = buildCreateRequest("Team Alpha", 99);
            when(ctx.bodyAsClass(CreateGruppoRequest.class)).thenReturn(req);
            when(userService.getProfile(99))
                    .thenThrow(new IllegalArgumentException("User non trovato"));

            invoke("createGruppo");

            verify(ctx).status(HttpStatus.NOT_FOUND);
            verify(gruppoService, never()).createGruppo(any(), any());
        }
    }

    // ═════════════════════════════════════════════
    // GET /api/gruppi  →  getAllGruppi()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/gruppi")
    class GetAllGruppi {

        @Test
        @DisplayName("200 OK: restituisce lista di gruppi")
        void shouldReturn200WithGruppiList() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            List<Gruppo> gruppi = List.of(
                    buildGruppo(1, "Team Alpha", owner),
                    buildGruppo(2, "Team Beta", owner)
            );
            when(gruppoService.getAllGruppi()).thenReturn(gruppi);

            invoke("getAllGruppi");

            verify(ctx).status(HttpStatus.OK);
            verify(ctx).json(gruppi);
        }

        @Test
        @DisplayName("200 OK: restituisce lista vuota se non ci sono gruppi")
        void shouldReturn200WithEmptyList() {
            when(gruppoService.getAllGruppi()).thenReturn(List.of());

            invoke("getAllGruppi");

            verify(ctx).status(HttpStatus.OK);
            verify(ctx).json(List.of());
        }
    }

    // ═════════════════════════════════════════════
    // GET /api/gruppi/{id}  →  getGruppoById()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/gruppi/{id}")
    class GetGruppoById {

        @Test
        @DisplayName("200 OK: restituisce il gruppo trovato")
        void shouldReturn200WhenGruppoExists() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            Gruppo gruppo = buildGruppo(1, "Team Alpha", owner);

            when(ctx.pathParam("id")).thenReturn("1");
            when(gruppoService.getGruppoById(1)).thenReturn(Optional.of(gruppo));

            invoke("getGruppoById");

            verify(ctx).status(HttpStatus.OK);
            verify(ctx).json(gruppo);
        }

        @Test
        @DisplayName("404 NOT FOUND: gruppo inesistente")
        void shouldReturn404WhenGruppoNotFound() {
            when(ctx.pathParam("id")).thenReturn("99");
            when(gruppoService.getGruppoById(99)).thenReturn(Optional.empty());

            invoke("getGruppoById");

            verify(ctx).status(HttpStatus.NOT_FOUND);
            verify(ctx, never()).json(any());
        }

        @Test
        @DisplayName("400 BAD REQUEST: id non numerico")
        void shouldReturn400WhenIdIsNotANumber() {
            when(ctx.pathParam("id")).thenReturn("abc");

            invoke("getGruppoById");

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(gruppoService, never()).getGruppoById(anyInt());
        }
    }

    // ═════════════════════════════════════════════
    // DELETE /api/gruppi/{id}  →  deleteGruppo()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("DELETE /api/gruppi/{id}")
    class DeleteGruppo {

        @Test
        @DisplayName("204 NO CONTENT: gruppo eliminato con successo")
        void shouldReturn204WhenGruppoIsDeleted() {
            when(ctx.pathParam("id")).thenReturn("1");
            doNothing().when(gruppoService).deleteGruppo(1);

            invoke("deleteGruppo");

            verify(ctx).status(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("404 NOT FOUND: gruppo inesistente")
        void shouldReturn404WhenGruppoNotFound() {
            when(ctx.pathParam("id")).thenReturn("99");
            doThrow(new IllegalArgumentException("Gruppo non trovato"))
                    .when(gruppoService).deleteGruppo(99);

            invoke("deleteGruppo");

            verify(ctx).status(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("400 BAD REQUEST: id non numerico")
        void shouldReturn400WhenIdIsNotANumber() {
            when(ctx.pathParam("id")).thenReturn("abc");

            invoke("deleteGruppo");

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(gruppoService, never()).deleteGruppo(anyInt());
        }
    }

    // ═════════════════════════════════════════════
    // POST /api/gruppi/{id}/membri  →  addMembro()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/gruppi/{id}/membri")
    class AddMembro {

        @Test
        @DisplayName("200 OK: membro aggiunto con successo")
        void shouldReturn200WhenMembroIsAdded() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            User newMember = buildUser(2, "Luigi", "luigi@example.com");
            Gruppo gruppo = buildGruppo(1, "Team Alpha", owner);
            gruppo.getMembri().add(newMember);
            MembroRequest req = buildMembroRequest(2);

            when(ctx.pathParam("id")).thenReturn("1");
            when(ctx.bodyAsClass(MembroRequest.class)).thenReturn(req);
            when(userService.getProfile(2)).thenReturn(newMember);
            when(gruppoService.addMembro(1, newMember)).thenReturn(gruppo);

            invoke("addMembro");

            verify(ctx).status(HttpStatus.OK);
            verify(ctx).json(gruppo);
        }

        @Test
        @DisplayName("404 NOT FOUND: utente inesistente")
        void shouldReturn404WhenUserNotFound() {
            MembroRequest req = buildMembroRequest(99);
            when(ctx.pathParam("id")).thenReturn("1");
            when(ctx.bodyAsClass(MembroRequest.class)).thenReturn(req);
            when(userService.getProfile(99))
                    .thenThrow(new IllegalArgumentException("User non trovato"));

            invoke("addMembro");

            verify(ctx).status(HttpStatus.NOT_FOUND);
            verify(gruppoService, never()).addMembro(anyInt(), any());
        }

        @Test
        @DisplayName("404 NOT FOUND: gruppo inesistente")
        void shouldReturn404WhenGruppoNotFound() {
            User user = buildUser(2, "Luigi", "luigi@example.com");
            MembroRequest req = buildMembroRequest(2);

            when(ctx.pathParam("id")).thenReturn("99");
            when(ctx.bodyAsClass(MembroRequest.class)).thenReturn(req);
            when(userService.getProfile(2)).thenReturn(user);
            when(gruppoService.addMembro(99, user))
                    .thenThrow(new IllegalArgumentException("Gruppo non trovato"));

            invoke("addMembro");

            verify(ctx).status(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("409 CONFLICT: utente già membro del gruppo")
        void shouldReturn409WhenAlreadyMember() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            MembroRequest req = buildMembroRequest(1);

            when(ctx.pathParam("id")).thenReturn("1");
            when(ctx.bodyAsClass(MembroRequest.class)).thenReturn(req);
            when(userService.getProfile(1)).thenReturn(owner);
            when(gruppoService.addMembro(1, owner))
                    .thenThrow(new IllegalStateException("Utente già membro del gruppo"));

            invoke("addMembro");

            verify(ctx).status(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("400 BAD REQUEST: id non numerico")
        void shouldReturn400WhenIdIsNotANumber() {
            when(ctx.pathParam("id")).thenReturn("abc");

            invoke("addMembro");

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(gruppoService, never()).addMembro(anyInt(), any());
        }
    }

    // ═════════════════════════════════════════════
    // DELETE /api/gruppi/{id}/membri  →  removeMembro()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("DELETE /api/gruppi/{id}/membri")
    class RemoveMembro {

        @Test
        @DisplayName("200 OK: membro rimosso con successo")
        void shouldReturn200WhenMembroIsRemoved() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            User member = buildUser(2, "Luigi", "luigi@example.com");
            Gruppo gruppo = buildGruppo(1, "Team Alpha", owner);
            MembroRequest req = buildMembroRequest(2);

            when(ctx.pathParam("id")).thenReturn("1");
            when(ctx.bodyAsClass(MembroRequest.class)).thenReturn(req);
            when(userService.getProfile(2)).thenReturn(member);
            when(gruppoService.removeMembro(1, member)).thenReturn(gruppo);

            invoke("removeMembro");

            verify(ctx).status(HttpStatus.OK);
            verify(ctx).json(gruppo);
        }

        @Test
        @DisplayName("404 NOT FOUND: utente inesistente")
        void shouldReturn404WhenUserNotFound() {
            MembroRequest req = buildMembroRequest(99);
            when(ctx.pathParam("id")).thenReturn("1");
            when(ctx.bodyAsClass(MembroRequest.class)).thenReturn(req);
            when(userService.getProfile(99))
                    .thenThrow(new IllegalArgumentException("User non trovato"));

            invoke("removeMembro");

            verify(ctx).status(HttpStatus.NOT_FOUND);
            verify(gruppoService, never()).removeMembro(anyInt(), any());
        }

        @Test
        @DisplayName("404 NOT FOUND: gruppo inesistente")
        void shouldReturn404WhenGruppoNotFound() {
            User user = buildUser(2, "Luigi", "luigi@example.com");
            MembroRequest req = buildMembroRequest(2);

            when(ctx.pathParam("id")).thenReturn("99");
            when(ctx.bodyAsClass(MembroRequest.class)).thenReturn(req);
            when(userService.getProfile(2)).thenReturn(user);
            when(gruppoService.removeMembro(99, user))
                    .thenThrow(new IllegalArgumentException("Gruppo non trovato"));

            invoke("removeMembro");

            verify(ctx).status(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("409 CONFLICT: tentativo di rimuovere l'owner")
        void shouldReturn409WhenRemovingOwner() {
            User owner = buildUser(1, "Mario", "mario@example.com");
            MembroRequest req = buildMembroRequest(1);

            when(ctx.pathParam("id")).thenReturn("1");
            when(ctx.bodyAsClass(MembroRequest.class)).thenReturn(req);
            when(userService.getProfile(1)).thenReturn(owner);
            when(gruppoService.removeMembro(1, owner))
                    .thenThrow(new IllegalStateException("Non puoi rimuovere il proprietario dal gruppo"));

            invoke("removeMembro");

            verify(ctx).status(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("400 BAD REQUEST: id non numerico")
        void shouldReturn400WhenIdIsNotANumber() {
            when(ctx.pathParam("id")).thenReturn("abc");

            invoke("removeMembro");

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(gruppoService, never()).removeMembro(anyInt(), any());
        }
    }
}