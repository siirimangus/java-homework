package com.example.homework.model;

import java.util.Arrays;
import java.util.Optional;

public enum NotificationType {
  EMAIL,
  SMS,
  PUSH;

  public static Optional<NotificationType> find(String value) {
    return Arrays.stream(values())
        .filter(type -> type.name().equals(value))
        .findFirst();
  }
}
