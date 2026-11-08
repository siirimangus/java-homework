package com.example.homework.dto;

import java.time.Instant;

import com.example.homework.model.NotificationStatus;

public record NotificationResponse(
    long id,
    String type,
    String target,
    String payload,
    NotificationStatus status,
    int failureCount,
    int dispatchCount,
    boolean blocked,
    String message,
    Instant createdAt,
    Instant updatedAt) {
}
