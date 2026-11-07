package com.example.homework.dto;

import java.time.Instant;

public record BlacklistEntryResponse(long id, String target, String reason, Instant createdAt) {
}
