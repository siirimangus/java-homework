package com.example.homework.exception;

/** The request is missing or has malformed data. */
public class InvalidNotificationRequestException extends NotificationException {

  public InvalidNotificationRequestException(String message) {
    super(message);
  }
}
