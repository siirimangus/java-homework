package com.example.homework.exception;

/** The recipient is already on the blacklist. */
public class BlacklistEntryAlreadyExistsException extends NotificationException {

  public BlacklistEntryAlreadyExistsException(String message) {
    super(message);
  }
}
