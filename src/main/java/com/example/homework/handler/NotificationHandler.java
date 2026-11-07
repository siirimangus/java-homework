package com.example.homework.handler;

import com.example.homework.model.NotificationType;

public interface NotificationHandler {

  /** The notification type this handler supports. */
  NotificationType getType();

  void execute(String target, String payload) throws Exception;
}
