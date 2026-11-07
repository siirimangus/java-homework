package com.example.homework.handler;

import com.example.homework.model.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationHandler implements NotificationHandler {

  @Override
  public NotificationType getType() {
    return NotificationType.PUSH;
  }

  @Override
  public void execute(String target, String payload) {
    System.out.println("[PUSH] " + target + " -> " + payload);
  }
}
