package com.example.user_service.unit;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.user_service.messaging.EventProducer;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.service.UserService;

import at.favre.lib.crypto.bcrypt.BCrypt;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Test unitari")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Mock
    private EventProducer eventProducer;

    // Helper: costruisce un User con password già hashata
    private User buildUser(int id, String nome, String email, String plainPassword) {
        User u = new User();
        u.setId(id);
        u.setNome(nome);
        u.setEmail(email);
        u.setPasswordHash(BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray()));
        u.setAdmin(false);
        return u;
    }

    // register()
    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("OK: salva utente con password hashata e non admin")
        void shouldRegisterUserSuccessfully() {
            when(userRepository.findByEmail("mario@example.com")).thenReturn(Optional.empty());
    
            // Simulo il comportamento del DB: quando salva, imposta un ID
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1); // <--- IMPORTANTE: assegna un ID per evitare NPE nel producer
                return u;
            });

            // 2. Esecuzione
            User result = userService.register("Mario", "mario@example.com", "password123");

            // 3. Assertions
            assertAll(
                () -> assertEquals("Mario", result.getNome()),
                () -> assertEquals(1, result.getId()), // Verifica che l'ID sia stato "settato"
                () -> assertFalse(result.isAdmin()),
                () -> assertNotEquals("password123", result.getPasswordHash())
            );

            // 4. VERIFICA l'evento (molto importante per la coverage!)
            verify(eventProducer, times(1)).sendEvent(eq("user.registered"), eq(1), anyMap());
            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("KO: email già registrata lancia IllegalStateException")
        void shouldThrowWhenEmailAlreadyExists() {
            User existing = buildUser(1, "Mario", "mario@example.com", "pass");
            when(userRepository.findByEmail("mario@example.com"))
                    .thenReturn(Optional.of(existing));

            assertThrows(IllegalStateException.class,
                    () -> userService.register("Luigi", "mario@example.com", "altropass"));

            // save NON deve mai essere chiamato
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("OK: il nuovo utente non è admin per default")
        void newUserShouldNotBeAdmin() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.register("Test", "test@example.com", "pass");

            assertFalse(result.isAdmin());
        }
    }

    // ═════════════════════════════════════════════
    // login()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("OK: credenziali corrette restituiscono l'utente")
        void shouldLoginWithCorrectCredentials() {
            User user = buildUser(1, "Mario", "mario@example.com", "password123");
            when(userRepository.findByEmail("mario@example.com"))
                    .thenReturn(Optional.of(user));

            User result = userService.login("mario@example.com", "password123");

            assertEquals("mario@example.com", result.getEmail());
        }

        @Test
        @DisplayName("KO: email inesistente lancia IllegalArgumentException")
        void shouldThrowWhenEmailNotFound() {
            when(userRepository.findByEmail("ghost@example.com"))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> userService.login("ghost@example.com", "password123"));
        }

        @Test
        @DisplayName("KO: password sbagliata lancia IllegalArgumentException")
        void shouldThrowWhenPasswordIsWrong() {
            User user = buildUser(1, "Mario", "mario@example.com", "passwordGiusta");
            when(userRepository.findByEmail("mario@example.com"))
                    .thenReturn(Optional.of(user));

            assertThrows(IllegalArgumentException.class,
                    () -> userService.login("mario@example.com", "passwordSbagliata"));
        }

        @Test
        @DisplayName("SICUREZZA: messaggio di errore identico per email e password errate")
        void shouldReturnSameErrorMessageForSecurity() {
            // Non deve rivelare se è l'email o la password ad essere sbagliata
            when(userRepository.findByEmail("ghost@example.com"))
                    .thenReturn(Optional.empty());
            User user = buildUser(1, "Mario", "mario@example.com", "giusta");
            when(userRepository.findByEmail("mario@example.com"))
                    .thenReturn(Optional.of(user));

            Exception exEmail = assertThrows(IllegalArgumentException.class,
                    () -> userService.login("ghost@example.com", "qualsiasi"));
            Exception exPass = assertThrows(IllegalArgumentException.class,
                    () -> userService.login("mario@example.com", "sbagliata"));

            assertEquals(exEmail.getMessage(), exPass.getMessage());
        }
    }

    // ═════════════════════════════════════════════
    // getProfile()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("OK: restituisce utente per id")
        void shouldReturnUserById() {
            User user = buildUser(1, "Mario", "mario@example.com", "pass");
            when(userRepository.findById(1)).thenReturn(Optional.of(user));

            User result = userService.getProfile(1);

            assertAll(
                () -> assertEquals(1, result.getId()),
                () -> assertEquals("Mario", result.getNome())
            );
        }

        @Test
        @DisplayName("KO: id inesistente lancia IllegalArgumentException")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> userService.getProfile(99));
        }
    }

    // ═════════════════════════════════════════════
    // getProfileByEmail()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("getProfileByEmail()")
    class GetProfileByEmail {

        @Test
        @DisplayName("OK: restituisce utente per email")
        void shouldReturnUserByEmail() {
            User user = buildUser(1, "Mario", "mario@example.com", "pass");
            when(userRepository.findByEmail("mario@example.com"))
                    .thenReturn(Optional.of(user));

            User result = userService.getProfileByEmail("mario@example.com");

            assertEquals("mario@example.com", result.getEmail());
        }

        @Test
        @DisplayName("KO: email inesistente lancia IllegalArgumentException")
        void shouldThrowWhenEmailNotFound() {
            when(userRepository.findByEmail("ghost@example.com"))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> userService.getProfileByEmail("ghost@example.com"));
        }
    }

    // ═════════════════════════════════════════════
    // getAllUsers()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("OK: restituisce lista completa di utenti")
        void shouldReturnAllUsers() {
            List<User> users = List.of(
                    buildUser(1, "Mario", "mario@example.com", "p1"),
                    buildUser(2, "Luigi", "luigi@example.com", "p2")
            );
            when(userRepository.findAll()).thenReturn(users);

            List<User> result = userService.getAllUsers();

            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("OK: restituisce lista vuota se non ci sono utenti")
        void shouldReturnEmptyListWhenNoUsers() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<User> result = userService.getAllUsers();

            assertTrue(result.isEmpty());
        }
    }

    // ═════════════════════════════════════════════
    // updateProfile()
    // ═════════════════════════════════════════════
    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("OK: aggiorna nome e email")
        void shouldUpdateNomeAndEmail() {
            User user = buildUser(1, "Mario", "mario@example.com", "pass");
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(userRepository.findByEmail("nuovo@example.com")).thenReturn(Optional.empty());
            when(userRepository.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateProfile(1, "Mario Rossi", "nuovo@example.com");

            assertAll(
                    () -> assertEquals("Mario Rossi", result.getNome()),
                    () -> assertEquals("nuovo@example.com", result.getEmail())
            );
        }

        @Test
        @DisplayName("OK: aggiorna solo il nome, email rimane invariata")
        void shouldUpdateOnlyNome() {
            User user = buildUser(1, "Mario", "mario@example.com", "pass");
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(userRepository.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateProfile(1, "Mario Rossi", null);

            assertEquals("Mario Rossi", result.getNome());
            assertEquals("mario@example.com", result.getEmail());
        }

        @Test
        @DisplayName("KO: email già in uso da altro utente lancia IllegalStateException")
        void shouldThrowWhenEmailTakenByAnotherUser() {
            User user  = buildUser(1, "Mario", "mario@example.com", "pass");
            User other = buildUser(2, "Luigi", "luigi@example.com", "pass");
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(userRepository.findByEmail("luigi@example.com")).thenReturn(Optional.of(other));

            assertThrows(IllegalStateException.class,
                    () -> userService.updateProfile(1, "Mario", "luigi@example.com"));

            // update NON deve mai essere chiamato
            verify(userRepository, never()).update(any());
        }

        @Test
        @DisplayName("OK: stesso utente può mantenere la propria email senza conflitto")
        void shouldAllowSameEmailForSameUser() {
            User user = buildUser(1, "Mario", "mario@example.com", "pass");
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(userRepository.findByEmail("mario@example.com")).thenReturn(Optional.of(user));
            when(userRepository.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> userService.updateProfile(1, "Mario", "mario@example.com"));
        }

        @Test
        @DisplayName("KO: utente inesistente lancia IllegalArgumentException")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(99)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> userService.updateProfile(99, "Nome", "email@example.com"));
        }

        @Test
        @DisplayName("OK: nome blank non sovrascrive il nome esistente")
        void shouldIgnoreBlankNome() {
            User user = buildUser(1, "Mario", "mario@example.com", "pass");
            when(userRepository.findById(1)).thenReturn(Optional.of(user));
            when(userRepository.update(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateProfile(1, "   ", null);

            assertEquals("Mario", result.getNome());
        }
    }
}
