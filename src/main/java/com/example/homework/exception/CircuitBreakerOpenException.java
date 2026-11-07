package com.example.homework.exception;

/** Too many consecutive failures; new dispatches are rejected. */
public class CircuitBreakerOpenException extends NotificationException {

  public CircuitBreakerOpenException(String message) {
    super(message);
  }
}
