package com.example.homework.util;

/** Masks recipient addresses so personal data does not end up in the logs. */
public final class TargetMasker {

  private TargetMasker() {
  }

  /** {@code buyer@example.com} becomes {@code b***@example.com}; other targets keep only their last two characters. */
  public static String mask(String target) {
    if (target == null || target.isBlank()) {
      return "***";
    }
    int at = target.indexOf('@');
    if (at > 0) {
      return target.charAt(0) + "***" + target.substring(at);
    }
    return target.length() > 4 ? "***" + target.substring(target.length() - 2) : "***";
  }
}
