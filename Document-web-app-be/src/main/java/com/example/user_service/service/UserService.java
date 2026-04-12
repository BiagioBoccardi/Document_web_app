package com.example.user_service.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.user_service.config.MetricsManager;
import com.example.user_service.messaging.EventProducer;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;

import at.favre.lib.crypto.bcrypt.BCrypt;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final EventProducer eventProducer;
    private final MetricsManager metricsManager;


    public UserService(UserRepository userRepository, EventProducer eventProducer, MetricsManager metricsManager) {
        this.userRepository = userRepository;
        this.eventProducer = eventProducer;
        this.metricsManager = metricsManager;
    }

    public User register(String nome, String email, String plainPassword) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            throw new IllegalStateException("Email già registrata");
        }

        User user = new User();
        user.setNome(nome);
        user.setEmail(email);
        user.setAdmin(false);

        String hash = BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray());
        user.setPasswordHash(hash);

        User savedUser = userRepository.save(user); 

        // Log di metrica di influxdb
        metricsManager.logEvent("user_registrations", "status", "success", "user_email", email);
        
        // Invia evento di registrazione a RabbitMQ
        eventProducer.sendEvent(
            "user.registered", 
            savedUser.getId(), 
            Map.of("nome", savedUser.getNome())
        );

        return savedUser;
    }

    public User login(String email, String plainPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Credenziali non valide"));

        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), user.getPasswordHash());
        if (!result.verified) {
            throw new IllegalArgumentException("Credenziali non valide");
        }

        return user;
    }

    public User getProfile(int userId) {           
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User non trovato"));
    }

    public User getProfileByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User non trovato"));
    }

    public List<User> getAllUsers() {               
        return userRepository.findAll();
    }

    public User updateProfile(int userId, String nome, String email) {  
        User user = getProfile(userId);
    
        if (nome != null && !nome.isBlank()) {
            user.setNome(nome);
        }
        if (email != null && !email.isBlank()) {
            Optional<User> conflict = userRepository.findByEmail(email);
            if (conflict.isPresent() && conflict.get().getId() != userId) { 
                throw new IllegalStateException("Email già in uso");
            }
            user.setEmail(email);
        }
    
        return userRepository.update(user);
    }
}