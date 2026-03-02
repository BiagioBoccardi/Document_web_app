package com.example.user_service.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
public class User {
    @Id
    private int id;
    private String nome;
    private String cognome;
    private String email;
    private String password;
    private boolean isAdmin;
}
