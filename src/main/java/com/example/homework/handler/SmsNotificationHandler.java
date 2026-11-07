package com.example.homework.handler;

import com.example.homework.model.NotificationType;
import com.example.homework.util.TargetMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationHandler implements NotificationHandler {

  private static final Logger log = LoggerFactory.getLogger(SmsNotificationHandler.class);

  @Override
  public NotificationType getType() {
    return NotificationType.SMS;
  }

  @Override
  public void execute(String target, String payload) {
    log.info("Sending SMS notification to {}", TargetMasker.mask(target));
  }
}
