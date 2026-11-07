package com.example.homework.exception;

/** The recipient is blacklisted or rate-limited. */
public class RecipientBlockedException extends NotificationException {

  public RecipientBlockedException(String message) {
    super(message);
  }
}
