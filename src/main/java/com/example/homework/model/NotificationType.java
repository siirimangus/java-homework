package com.example.homework.model;

import java.util.Arrays;
import java.util.Optional;

/** Notification channels the service can dispatch to. */
public enum NotificationType {
  EMAIL,
  SMS,
  PUSH;

  /** Resolves a type by its name, or empty if the channel is not supported. */
  public static Optional<NotificationType> find(String value) {
    return Arrays.stream(values())
        .filter(type -> type.name().equals(value))
        .findFirst();
  }
}
