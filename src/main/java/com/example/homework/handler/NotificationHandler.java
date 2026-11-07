package com.example.homework.handler;

public interface NotificationHandler {

  /** The notification type this handler supports, e.g. {@code EMAIL}. */
  String getType();

  void execute(String target, String payload) throws Exception;
}
