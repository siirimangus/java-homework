package com.example.homework.handler;

import org.springframework.stereotype.Component;

@Component
public class SmsNotificationHandler implements NotificationHandler {

  @Override
  public String getType() {
    return "SMS";
  }

  @Override
  public void execute(String target, String payload) {
    System.out.println("[SMS] " + target + " -> " + payload);
  }
}
