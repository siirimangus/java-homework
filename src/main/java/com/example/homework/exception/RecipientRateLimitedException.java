package com.example.homework.exception;

/** The recipient received too many notifications within the rate-limit window. */
public class RecipientRateLimitedException extends NotificationException {

  public RecipientRateLimitedException(String message) {
    super(message);
  }
}
