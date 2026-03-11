package com.example.user_service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

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
    @JsonIgnore // Non inviare mai la password al frontend
    private String password;

    @Column(name = "isadmin", nullable = false)
    private boolean isAdmin;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    // Metodo per creare un oggetto User "pulito" da inviare al client
    public User forClient() {
        User clientUser = new User();
        clientUser.setId(this.id);
        clientUser.setNome(this.nome);
        clientUser.setCognome(this.cognome);
        clientUser.setEmail(this.email);
        clientUser.setAdmin(this.isAdmin);
        return clientUser;
    }
}
