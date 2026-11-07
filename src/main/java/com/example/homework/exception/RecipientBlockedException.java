package com.example.homework.exception;

/** The recipient is blacklisted. */
public class RecipientBlockedException extends NotificationException {

  public RecipientBlockedException(String message) {
    super(message);
  }
}
