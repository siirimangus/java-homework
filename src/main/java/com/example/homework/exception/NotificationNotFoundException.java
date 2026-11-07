package com.example.homework.exception;

/** No notification exists with the requested id. */
public class NotificationNotFoundException extends NotificationException {

  public NotificationNotFoundException(String message) {
    super(message);
  }
}
