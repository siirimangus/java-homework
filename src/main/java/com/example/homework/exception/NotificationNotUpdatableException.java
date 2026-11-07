package com.example.homework.exception;

/** The notification is in a state in which it can not be edited. */
public class NotificationNotUpdatableException extends NotificationException {

  public NotificationNotUpdatableException(String message) {
    super(message);
  }
}
