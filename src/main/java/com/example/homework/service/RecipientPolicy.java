package com.example.homework.service;

import java.time.Clock;
import java.time.Instant;
import com.example.homework.model.NotificationType;

import com.example.homework.config.RateLimitProperties;
import com.example.homework.exception.RecipientBlockedException;
import com.example.homework.exception.RecipientRateLimitedException;
import com.example.homework.repository.BlacklistRepository;
import com.example.homework.repository.NotificationRepository;
import com.example.homework.util.TargetMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Decides whether a recipient may receive a notification right now. */
@Component
public class RecipientPolicy {

  private static final Logger log = LoggerFactory.getLogger(RecipientPolicy.class);

  private final BlacklistRepository blacklistRepository;
  private final NotificationRepository notificationRepository;
  private final RateLimitProperties rateLimit;
  private final Clock clock;

  public RecipientPolicy(
      BlacklistRepository blacklistRepository,
      NotificationRepository notificationRepository,
      RateLimitProperties rateLimit,
      Clock clock) {
    this.blacklistRepository = blacklistRepository;
    this.notificationRepository = notificationRepository;
    this.rateLimit = rateLimit;
    this.clock = clock;
  }

  public void enforce(String type, String target) {
    // Only email addresses are case-insensitive; SMS numbers and push tokens are matched exactly.
    boolean ignoreCase = NotificationType.EMAIL.name().equals(type);

    if (blacklistRepository.existsByTarget(target, ignoreCase)) {
      log.warn("Rejected notification to {}: recipient is blacklisted", TargetMasker.mask(target));
      throw new RecipientBlockedException("Recipient is blacklisted.");
    }

    Instant windowStart = clock.instant().minus(rateLimit.window());
    if (notificationRepository.countDispatchesSince(target, ignoreCase, windowStart) >= rateLimit.maxRequests()) {
      log.warn("Rejected notification to {}: recipient is rate-limited", TargetMasker.mask(target));
      throw new RecipientRateLimitedException("Recipient is rate-limited: too many notifications, try again later.");
    }
  }
}
