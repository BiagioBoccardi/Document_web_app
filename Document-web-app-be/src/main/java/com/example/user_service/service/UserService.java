package com.example.user_service.service;

<<<<<<< HEAD
import at.favre.lib.crypto.bcrypt.BCrypt;
import com.example.user_service.models.User;
import com.example.user_service.persistence.UserDao;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
=======
>>>>>>> a31fb12f1dc53f77edc7c168491f544774be7500
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

<<<<<<< HEAD
=======
import org.mindrot.jbcrypt.BCrypt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;

>>>>>>> a31fb12f1dc53f77edc7c168491f544774be7500
public class UserService {

    private final UserDao userDao;
    private final String jwtSecret;

    public UserService(UserDao userDao, String jwtSecret) {
        this.userDao = userDao;
        this.jwtSecret = jwtSecret;
    }

<<<<<<< HEAD
    public User register(String nome, String cognome, String email, String plainPassword) {
        Optional<User> existingUser = userDao.findByEmail(email);
=======
    public User register(String nome, String email, String plainPassword, boolean isAdmin) {
        Optional<User> existingUser = userRepository.findByEmail(email);
>>>>>>> a31fb12f1dc53f77edc7c168491f544774be7500
        if (existingUser.isPresent()) {
            throw new IllegalStateException("Email già registrata");
        }

        User user = new User();
        user.setNome(nome);
        user.setEmail(email);
<<<<<<< HEAD
        user.setPassword(BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray()));
        user.setAdmin(false);
=======
        user.setAdmin(isAdmin);
        user.setPasswordHash(BCrypt.hashpw(plainPassword, BCrypt.gensalt(12)));
>>>>>>> a31fb12f1dc53f77edc7c168491f544774be7500

        return userDao.save(user);
    }

<<<<<<< HEAD
    public String login(String email, String plainPassword) {
        User user = userDao.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenziali non valide"));

        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), user.getPassword());
        if (!result.verified) {
=======
    public User login(String email, String plainPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenziali non valide"));

        if (!BCrypt.checkpw(plainPassword, user.getPasswordHash())) {
>>>>>>> a31fb12f1dc53f77edc7c168491f544774be7500
            throw new IllegalArgumentException("Credenziali non valide");
        }

        return user;
    }

    public User getProfile(long userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User non trovato"));
    }

    public User getProfileByEmail(String email) {
        return userDao.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User non trovato"));
    }

<<<<<<< HEAD
    public User updateProfile(long userId, String nome, String cognome) {
        User user = getProfile(userId);
        user.setNome(nome);
        user.setCognome(cognome);
        userDao.update(user);
        return user;
=======
    public User updateProfile(int userId, String nome) {
        User user = getProfile(userId);
        user.setNome(nome);
        return userRepository.update(user);
>>>>>>> a31fb12f1dc53f77edc7c168491f544774be7500
    }

    public String generateJwtToken(User utente) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            return JWT.create()
                    .withSubject(String.valueOf(utente.getId()))
                    .withClaim("email", utente.getEmail())
                    .withClaim("role", utente.isAdmin() ? "ADMIN" : "USER")
                    .withIssuedAt(Date.from(Instant.now()))
                    .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                    .sign(algorithm);
        } catch (JWTCreationException ex) {
            throw new IllegalStateException("Errore nella generazione del token JWT", ex);
        }
    }
}