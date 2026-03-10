package com.example.notification_service.entity;

import java.time.LocalDateTime;
import java.util.UUID;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
@Table(name = "notifiche")
public class Notification {
    @Id
    @GeneratedValue
    private UUID uuid;
    @Column(nullable = false)
    private String messaggio;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column( nullable = false)
    private String stato; // "inviata", "consegnata", "letta", "errore consegna"
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.stato == null) this.stato = "PENDING";
    }
}