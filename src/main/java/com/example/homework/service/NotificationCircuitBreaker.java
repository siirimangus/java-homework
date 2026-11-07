package com.example.homework.service;

import java.time.Clock;
import java.time.Instant;

import com.example.homework.config.CircuitBreakerProperties;
import com.example.homework.repository.NotificationRepository;
import org.springframework.stereotype.Component;

/**
 * Rejects new dispatches after repeated delivery failures.
 *
 * <p>State is derived from the persisted outcome of the last completed delivery, so it is shared
 * by all instances. Rejected and unsupported attempts never change it. Once the cooldown has
 * passed the breaker is half-open: dispatches are let through, a success closes it and a failure
 * opens it again for another cooldown.
 */
@Component
public class NotificationCircuitBreaker {

  private final NotificationRepository repository;
  private final CircuitBreakerProperties properties;
  private final Clock clock;

  public NotificationCircuitBreaker(
      NotificationRepository repository, CircuitBreakerProperties properties, Clock clock) {
    this.repository = repository;
    this.properties = properties;
    this.clock = clock;
  }

  public boolean isOpen() {
    return repository.getLastDeliveryOutcome()
        .filter(outcome -> outcome.failureCount() >= properties.failureThreshold())
        .map(outcome -> outcome.at().plus(properties.cooldown()))
        .map(reopensAt -> clock.instant().isBefore(reopensAt))
        .orElse(false);
  }
}
