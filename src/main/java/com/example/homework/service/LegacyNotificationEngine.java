package com.example.homework.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.homework.dto.LegacyNotificationResponse;
import com.example.homework.handler.NotificationHandler;
import com.example.homework.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class LegacyNotificationEngine {

  private final NotificationRepository repository;
  private final Map<String, NotificationHandler> handlers = new HashMap<>();

  public LegacyNotificationEngine(NotificationRepository repository, List<NotificationHandler> handlers) {
    this.repository = repository;
    for (NotificationHandler handler : handlers) {
      this.handlers.put(handler.getType(), handler);
    }
  }

  public synchronized LegacyNotificationResponse processNotification(String type, String target, String payload)
      throws Exception {

    String safeType = type != null ? type.trim().toUpperCase() : "";
    String safeTarget = target != null ? target.trim() : null;
    String safePayload = payload != null ? payload : "";

    if (safeTarget == null || safeTarget.length() == 0) {
      throw new IllegalArgumentException("Target is mandatory.");
    }

    if (repository.isUserBlacklistedOrRateLimited(safeTarget)) {
      throw new RuntimeException("Business Rule Violation: User is blacklisted or rate-limited by DB policy.");
    }

    long id = repository.openDispatch(safeType, safeTarget, safePayload);

    if (repository.getFailureCountFromDatabase() >= 3) {
      repository.markAsSkipped(id, "Circuit breaker OPEN via database state.");
      throw new RuntimeException("Circuit breaker OPEN via database state.");
    }

    NotificationHandler handler = handlers.get(safeType);
    if (handler == null) {
      System.out.println("LOGGING TO DB: Unknown type attempted -> " + type);
      repository.recordUnsupportedType(id, safeType);
      throw new Exception("Unsupported type: " + type);
    }

    try {
      handler.execute(safeTarget, safePayload);
      repository.resetDatabaseFailures(id);
      return repository.getDispatchById(id);
    } catch (Exception e) {
      repository.recordDatabaseFailure(id, safeTarget, e.getMessage());
      throw e;
    }
  }

  public List<LegacyNotificationResponse> getHistory() {
    return repository.getHistory();
  }

  public LegacyNotificationResponse getLatestState() {
    return repository.getLatestState();
  }
}
