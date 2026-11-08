package com.example.homework.exception;

public class BlacklistEntryAlreadyExistsException extends NotificationException {

  public BlacklistEntryAlreadyExistsException(String message) {
    super(message);
  }
}
