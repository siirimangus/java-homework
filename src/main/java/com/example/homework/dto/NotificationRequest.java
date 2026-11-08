package com.example.homework.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NotificationRequest(
    @Size(max = 32, message = "Type must be at most 32 characters.")
    String type,
    @NotBlank(message = "Target is mandatory.")
    @Size(max = 255, message = "Target must be at most 255 characters.")
    String target,
    String payload) {
}
