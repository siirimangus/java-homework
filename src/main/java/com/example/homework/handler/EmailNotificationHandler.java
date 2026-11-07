package com.example.homework.handler;

import com.example.homework.model.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationHandler implements NotificationHandler {

  @Override
  public NotificationType getType() {
    return NotificationType.EMAIL;
  }

  @Override
  public void execute(String target, String payload) {
    if (target != null && target.contains("error")) {
      throw new RuntimeException("SMTP Timeout");
    }
    System.out.println("[EMAIL] " + target + " -> " + payload);
  }
}
