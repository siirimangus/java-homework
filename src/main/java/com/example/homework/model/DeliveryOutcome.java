package com.example.homework.model;

import java.time.Instant;

/**
 * The most recent completed delivery attempt.
 *
 * @param failureCount consecutive failed deliveries as of that attempt (0 after a success)
 * @param at when the attempt finished
 */
public record DeliveryOutcome(int failureCount, Instant at) {
}
