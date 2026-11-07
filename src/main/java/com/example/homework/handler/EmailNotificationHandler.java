package com.example.homework.handler;

import org.springframework.stereotype.Component;

@Component
public class EmailNotificationHandler implements NotificationHandler {

  @Override
  public String getType() {
    return "EMAIL";
  }

  @Override
  public void execute(String target, String payload) {
    if (target != null && target.contains("error")) {
      throw new RuntimeException("SMTP Timeout");
    }
    System.out.println("[EMAIL] " + target + " -> " + payload);
  }
}
