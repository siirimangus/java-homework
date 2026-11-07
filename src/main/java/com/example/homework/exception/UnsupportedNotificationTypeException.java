package com.example.homework.exception;

/** The requested notification type has no registered handler. */
public class UnsupportedNotificationTypeException extends NotificationException {

  public UnsupportedNotificationTypeException(String message) {
    super(message);
  }
}
