package com.example.homework.handler;

import com.example.homework.model.NotificationType;

public interface NotificationHandler {

  NotificationType getType();

  void execute(String target, String payload) throws Exception;
}
