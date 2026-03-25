package com.example.user_service.integration;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.example.user_service.controller.UserController;
import com.example.user_service.dto.LoginRequest;
import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.model.User;
import com.example.user_service.service.UserService;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)   // evita UnnecessaryStubbingException
@DisplayName("UserController - Test unitari")
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Context ctx;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setupCtxChain() {
        // ctx.status(...) deve restituire ctx stesso per supportare la catena
        // .status(HttpStatus.CREATED).json(user)
        lenient().when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────
    private RegisterRequest buildRegisterRequest(String nome, String email, String password) {
        RegisterRequest req = new RegisterRequest();
        req.nome = nome;
        req.email = email;
        req.passwordHash = password;
        return req;
    }

    private LoginRequest buildLoginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.email = email;
        req.passwordHash = password;
        return req;
    }

    private User buildUser(int id, String nome, String email) {
        User u = new User();
        u.setId(id);
        u.setNome(nome);
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setAdmin(false);
        return u;
    }

    // ═════════════════════════════════════════════
    // POST /api/v1/users/sign-up  →  register()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/v1/users/sign-up")
    class Register {

        @Test
        @DisplayName("201 CREATED: registrazione avvenuta con successo")
        void shouldReturn201WhenRegistrationIsSuccessful() {
            RegisterRequest req = buildRegisterRequest("Mario", "mario@example.com", "password123");
            User savedUser = buildUser(1, "Mario", "mario@example.com");

            when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(req);
            when(userService.register("Mario", "mario@example.com", "password123"))
                    .thenReturn(savedUser);

            invokeRegister();

            verify(ctx).status(HttpStatus.CREATED);
            verify(ctx).json(savedUser);
        }

        @Test
        @DisplayName("400 BAD REQUEST: nome mancante")
        void shouldReturn400WhenNomeIsNull() {
            RegisterRequest req = buildRegisterRequest(null, "mario@example.com", "password123");
            when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(req);

            invokeRegister();

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(userService, never()).register(any(), any(), any());
        }

        @Test
        @DisplayName("400 BAD REQUEST: nome blank")
        void shouldReturn400WhenNomeIsBlank() {
            RegisterRequest req = buildRegisterRequest("   ", "mario@example.com", "password123");
            when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(req);

            invokeRegister();

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(userService, never()).register(any(), any(), any());
        }

        @Test
        @DisplayName("400 BAD REQUEST: email mancante")
        void shouldReturn400WhenEmailIsNull() {
            RegisterRequest req = buildRegisterRequest("Mario", null, "password123");
            when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(req);

            invokeRegister();

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(userService, never()).register(any(), any(), any());
        }

        @Test
        @DisplayName("400 BAD REQUEST: password mancante")
        void shouldReturn400WhenPasswordIsNull() {
            RegisterRequest req = buildRegisterRequest("Mario", "mario@example.com", null);
            when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(req);

            invokeRegister();

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(userService, never()).register(any(), any(), any());
        }

        @Test
        @DisplayName("409 CONFLICT: email già registrata")
        void shouldReturn409WhenEmailAlreadyExists() {
            RegisterRequest req = buildRegisterRequest("Mario", "mario@example.com", "password123");
            when(ctx.bodyAsClass(RegisterRequest.class)).thenReturn(req);
            when(userService.register("Mario", "mario@example.com", "password123"))
                    .thenThrow(new IllegalStateException("Email già registrata"));

            invokeRegister();

            verify(ctx).status(HttpStatus.CONFLICT);
            verify(ctx).json(any());
        }

        private void invokeRegister() {
            try {
                var method = UserController.class.getDeclaredMethod("register", Context.class);
                method.setAccessible(true);
                method.invoke(userController, ctx);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ═════════════════════════════════════════════
    // POST /api/v1/users/sign-in  →  login()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/v1/users/sign-in")
    class Login {

        @Test
        @DisplayName("200 OK: login avvenuto con successo")
        void shouldReturn200WhenLoginIsSuccessful() {
            LoginRequest req = buildLoginRequest("mario@example.com", "password123");
            User user = buildUser(1, "Mario", "mario@example.com");
            ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);

            when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(req);
            when(userService.login("mario@example.com", "password123")).thenReturn(user);

            invokeLogin();

            verify(ctx).status(HttpStatus.OK);
            verify(ctx).json(captor.capture());

            Map<String, Object> response = (Map<String, Object>) captor.getValue();
            
            assertEquals(user, response.get("user"), "L'utente nel JSON deve essere quello corretto");
            assertNotNull(response.get("token"), "Il token non deve essere nullo");
        }

        @Test
        @DisplayName("400 BAD REQUEST: email mancante")
        void shouldReturn400WhenEmailIsNull() {
            LoginRequest req = buildLoginRequest(null, "password123");
            when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(req);

            invokeLogin();

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(userService, never()).login(any(), any());
        }

        @Test
        @DisplayName("400 BAD REQUEST: email blank")
        void shouldReturn400WhenEmailIsBlank() {
            LoginRequest req = buildLoginRequest("   ", "password123");
            when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(req);

            invokeLogin();

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(userService, never()).login(any(), any());
        }

        @Test
        @DisplayName("400 BAD REQUEST: password mancante")
        void shouldReturn400WhenPasswordIsNull() {
            LoginRequest req = buildLoginRequest("mario@example.com", null);
            when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(req);

            invokeLogin();

            verify(ctx).status(HttpStatus.BAD_REQUEST);
            verify(userService, never()).login(any(), any());
        }

        @Test
        @DisplayName("401 UNAUTHORIZED: credenziali errate")
        void shouldReturn401WhenCredentialsAreWrong() {
            LoginRequest req = buildLoginRequest("mario@example.com", "passwordSbagliata");
            when(ctx.bodyAsClass(LoginRequest.class)).thenReturn(req);
            when(userService.login("mario@example.com", "passwordSbagliata"))
                    .thenThrow(new IllegalArgumentException("Credenziali non valide"));

            invokeLogin();

            verify(ctx).status(HttpStatus.UNAUTHORIZED);
            verify(ctx).json(any());
        }

        private void invokeLogin() {
            try {
                var method = UserController.class.getDeclaredMethod("login", Context.class);
                method.setAccessible(true);
                method.invoke(userController, ctx);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}