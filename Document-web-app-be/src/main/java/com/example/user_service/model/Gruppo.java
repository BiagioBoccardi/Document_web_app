package com.example.user_service.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.ArrayList;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "gruppo")
public class Gruppo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
        name = "gruppo_membri",
        joinColumns = @JoinColumn(name = "gruppo_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> membri = new ArrayList<>();

   
}