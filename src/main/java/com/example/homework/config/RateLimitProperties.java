package com.example.homework.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Per-recipient rate limit: at most {@code maxRequests} notifications within the sliding {@code window}. */
@ConfigurationProperties("notification.rate-limit")
public record RateLimitProperties(
    @DefaultValue("3") int maxRequests,
    @DefaultValue("1m") Duration window) {
}
