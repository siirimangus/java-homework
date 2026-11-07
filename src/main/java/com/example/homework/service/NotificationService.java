package com.example.homework.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.example.homework.dto.NotificationResponse;
import com.example.homework.exception.CircuitBreakerOpenException;
import com.example.homework.exception.InvalidNotificationRequestException;
import com.example.homework.exception.NotificationDeliveryException;
import com.example.homework.exception.RecipientBlockedException;
import com.example.homework.exception.UnsupportedNotificationTypeException;
import com.example.homework.handler.NotificationHandler;
import com.example.homework.model.NotificationType;
import com.example.homework.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationRepository repository;
  private final TransactionTemplate transactionTemplate;
  private final Map<NotificationType, NotificationHandler> handlers = new EnumMap<>(NotificationType.class);

  public NotificationService(
      NotificationRepository repository,
      List<NotificationHandler> handlers,
      TransactionTemplate transactionTemplate) {
    this.repository = repository;
    this.transactionTemplate = transactionTemplate;
    for (NotificationHandler handler : handlers) {
      this.handlers.put(handler.getType(), handler);
    }
  }

  /**
   * Dispatches a notification. The request is admitted in one short transaction, delivery runs
   * outside any transaction (so slow channels do not hold a database connection or a lock), and
   * the outcome is recorded with a single atomic statement.
   */
  public NotificationResponse processNotification(String type, String target, String payload) {

    String safeType = type != null ? type.trim().toUpperCase() : "";
    String safeTarget = target != null ? target.trim() : null;
    String safePayload = payload != null ? payload : "";

    if (safeTarget == null || safeTarget.length() == 0) {
      throw new InvalidNotificationRequestException("Target is mandatory.");
    }

    Dispatch dispatch = transactionTemplate.execute(status -> admit(safeType, safeTarget, safePayload));

    // Rejections are thrown after the admission transaction has committed so the row is kept.
    if (dispatch.failureCount() >= 3) {
      repository.markAsSkipped(dispatch.id(), "Circuit breaker OPEN via database state.");
      log.warn("Dispatch {} skipped: circuit breaker is open", dispatch.id());
      throw new CircuitBreakerOpenException("Circuit breaker OPEN via database state.");
    }

    NotificationHandler handler = NotificationType.find(safeType).map(handlers::get).orElse(null);
    if (handler == null) {
      repository.recordUnsupportedType(dispatch.id(), safeType);
      log.warn("Dispatch {} has unsupported type: {}", dispatch.id(), safeType);
      throw new UnsupportedNotificationTypeException("Unsupported type: " + type);
    }

    try {
      handler.execute(safeTarget, safePayload);
      repository.resetDatabaseFailures(dispatch.id());
      log.info("Dispatch {} sent ({} to {})", dispatch.id(), safeType, safeTarget);
      return repository.getDispatchById(dispatch.id());
    } catch (Exception e) {
      repository.recordDatabaseFailure(dispatch.id(), e.getMessage());
      log.warn("Dispatch {} failed ({} to {})", dispatch.id(), safeType, safeTarget, e);
      throw new NotificationDeliveryException(e.getMessage(), e);
    }
  }

  public List<NotificationResponse> getHistory() {
    return repository.getHistory();
  }

  public NotificationResponse getLatestState() {
    return repository.getLatestState();
  }

  /** Runs inside a transaction: enforces the recipient policy and records the new dispatch. */
  private Dispatch admit(String type, String target, String payload) {
    if (repository.isUserBlacklistedOrRateLimited(target)) {
      log.warn("Rejected notification to {}: recipient is blocked or rate-limited", target);
      throw new RecipientBlockedException("Business Rule Violation: User is blacklisted or rate-limited by DB policy.");
    }
    long id = repository.openDispatch(type, target, payload);
    return new Dispatch(id, repository.getDispatchById(id).getFailureCount());
  }

  private record Dispatch(long id, int failureCount) {
  }
}
