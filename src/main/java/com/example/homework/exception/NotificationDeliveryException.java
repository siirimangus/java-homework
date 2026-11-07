package com.example.homework.exception;

/** The channel handler failed to deliver the notification. */
public class NotificationDeliveryException extends NotificationException {

  public NotificationDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
