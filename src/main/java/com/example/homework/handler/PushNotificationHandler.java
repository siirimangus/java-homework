package com.example.homework.handler;

import com.example.homework.model.NotificationType;
import com.example.homework.util.TargetMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationHandler implements NotificationHandler {

  private static final Logger log = LoggerFactory.getLogger(PushNotificationHandler.class);

  @Override
  public NotificationType getType() {
    return NotificationType.PUSH;
  }

  @Override
  public void execute(String target, String payload) {
    log.info("Sending PUSH notification to {}", TargetMasker.mask(target));
  }
}
