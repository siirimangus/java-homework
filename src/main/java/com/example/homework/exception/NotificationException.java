package com.example.homework.exception;

/** Base type for all expected, business-level failures of the notification service. */
public abstract class NotificationException extends RuntimeException {

  protected NotificationException(String message) {
    super(message);
  }

  protected NotificationException(String message, Throwable cause) {
    super(message, cause);
  }
}
