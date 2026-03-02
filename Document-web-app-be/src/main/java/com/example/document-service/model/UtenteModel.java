package com.example.document.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UtenteModel {
    private int id;
    private String nome;
    private String cognome;
    private String email;
    private String password;
    private boolean isAdmin;
}
