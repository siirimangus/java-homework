package com.example.homework.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Circuit breaker settings.
 *
 * @param failureThreshold consecutive failed deliveries after which new dispatches are rejected
 * @param cooldown how long dispatches are rejected before a trial dispatch is let through
 */
@ConfigurationProperties("notification.circuit-breaker")
public record CircuitBreakerProperties(
    @DefaultValue("3") int failureThreshold,
    @DefaultValue("30s") Duration cooldown) {
}
