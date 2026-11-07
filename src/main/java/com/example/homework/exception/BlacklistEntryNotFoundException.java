package com.example.homework.exception;

/** No blacklist entry exists with the requested id. */
public class BlacklistEntryNotFoundException extends NotificationException {

  public BlacklistEntryNotFoundException(String message) {
    super(message);
  }
}
