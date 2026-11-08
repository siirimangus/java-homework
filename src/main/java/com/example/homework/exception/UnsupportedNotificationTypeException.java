package com.example.homework.exception;

public class UnsupportedNotificationTypeException extends InvalidNotificationRequestException {

  public UnsupportedNotificationTypeException(String message) {
    super(message);
  }
}
