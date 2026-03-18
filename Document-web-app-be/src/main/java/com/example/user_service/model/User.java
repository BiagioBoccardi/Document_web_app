    package com.example.user_service.model;

    import com.fasterxml.jackson.annotation.JsonIgnore;

    import jakarta.persistence.Column;
    import jakarta.persistence.Entity;
    import jakarta.persistence.GeneratedValue;
    import jakarta.persistence.GenerationType;
    import jakarta.persistence.Id;
    import jakarta.persistence.Table;
    import lombok.AllArgsConstructor;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    @Data
    @NoArgsConstructor          
    @AllArgsConstructor
    @Entity
    @Table(name = "utente")
    public class User {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private int id;         

        @Column(nullable = false)
        private String nome;

        @Column(nullable = false, unique = true)
        private String email;

        @Column(nullable = false)
        @JsonIgnore
        private String passwordHash;

        @Column(name = "is_admin", nullable = false)
        private boolean isAdmin;
    }