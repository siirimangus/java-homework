package com.example.homework.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update of a stored notification. A field that is absent (null) is left unchanged.
 *
 * @param target new destination, if it should change
 * @param payload new content, if it should change
 */
public record NotificationUpdateRequest(
    @Pattern(regexp = "(?s).*\\S.*", message = "Target must not be blank.")
    @Size(max = 255, message = "Target must be at most 255 characters.")
    String target,
    String payload) {
}
