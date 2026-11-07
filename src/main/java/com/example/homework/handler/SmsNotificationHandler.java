package com.example.homework.handler;

import com.example.homework.model.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationHandler implements NotificationHandler {

  @Override
  public NotificationType getType() {
    return NotificationType.SMS;
  }

  @Override
  public void execute(String target, String payload) {
    System.out.println("[SMS] " + target + " -> " + payload);
  }
}
