package com.example.user_service.model;

import jakarta.persistence.Entity;
<<<<<<<< HEAD:Document-web-app-be/src/main/java/com/example/user_service/model/UtenteModel.java
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
========
>>>>>>>> f6285c2b857ee000d17288c2e1fdd1fe77991e43:Document-web-app-be/src/main/java/com/example/user_service/model/User.java
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
<<<<<<<< HEAD:Document-web-app-be/src/main/java/com/example/user_service/model/UtenteModel.java
public class UtenteModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
========
public class User {
    @Id
>>>>>>>> f6285c2b857ee000d17288c2e1fdd1fe77991e43:Document-web-app-be/src/main/java/com/example/user_service/model/User.java
    private int id;
    private String nome;
    private String cognome;
    private String email;
    private String password;
    private boolean isAdmin;
}
