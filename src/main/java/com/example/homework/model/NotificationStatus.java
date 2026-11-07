package com.example.homework.model;

import java.util.Arrays;

/** Lifecycle state of a notification dispatch, persisted as a single-character code. */
public enum NotificationStatus {
  NEW("N"),
  SENT("S"),
  ERROR("E"),
  UNKNOWN("U"),
  REJECTED("R");

  private final String code;

  NotificationStatus(String code) {
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  public static NotificationStatus fromCode(String code) {
    return Arrays.stream(values())
        .filter(status -> status.code.equals(code))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown notification status code: " + code));
  }
}
