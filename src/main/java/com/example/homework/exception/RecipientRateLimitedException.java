package com.example.homework.exception;

public class RecipientRateLimitedException extends NotificationException {

  public RecipientRateLimitedException(String message) {
    super(message);
  }
}
