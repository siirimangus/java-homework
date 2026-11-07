package com.example.homework.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlacklistRequest(
    @NotBlank(message = "Target is mandatory.")
    @Size(max = 255, message = "Target must be at most 255 characters.")
    String target,
    @Size(max = 255, message = "Reason must be at most 255 characters.")
    String reason) {
}
