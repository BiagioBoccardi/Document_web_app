package com.example.notification_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequest {
    private int userId;
    private String messaggio;
    private String stato; // `PENDING`, `SENT`, `READ`, `FAILED`
}
