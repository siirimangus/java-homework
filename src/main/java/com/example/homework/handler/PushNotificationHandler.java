package com.example.homework.handler;

import org.springframework.stereotype.Component;

@Component
public class PushNotificationHandler implements NotificationHandler {

  @Override
  public String getType() {
    return "PUSH";
  }

  @Override
  public void execute(String target, String payload) {
    System.out.println("[PUSH] " + target + " -> " + payload);
  }
}
