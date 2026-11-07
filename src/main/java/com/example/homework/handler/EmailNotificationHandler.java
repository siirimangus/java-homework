package com.example.homework.handler;

import com.example.homework.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationHandler implements NotificationHandler {

  private static final Logger log = LoggerFactory.getLogger(EmailNotificationHandler.class);

  @Override
  public NotificationType getType() {
    return NotificationType.EMAIL;
  }

  @Override
  public void execute(String target, String payload) {
    if (target != null && target.contains("error")) {
      throw new RuntimeException("SMTP Timeout");
    }
    log.info("Sending EMAIL notification to {}", target);
  }
}
