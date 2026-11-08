package com.example.homework.exception;

public class CircuitBreakerOpenException extends NotificationException {

  public CircuitBreakerOpenException(String message) {
    super(message);
  }
}
