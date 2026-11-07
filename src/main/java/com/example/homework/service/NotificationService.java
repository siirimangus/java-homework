package com.example.homework.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.example.homework.dto.NotificationResponse;
import com.example.homework.dto.NotificationUpdateRequest;
import com.example.homework.exception.CircuitBreakerOpenException;
import com.example.homework.exception.InvalidNotificationRequestException;
import com.example.homework.exception.NotificationDeliveryException;
import com.example.homework.exception.NotificationException;
import com.example.homework.exception.NotificationNotFoundException;
import com.example.homework.exception.NotificationNotUpdatableException;
import com.example.homework.exception.RecipientBlockedException;
import com.example.homework.exception.RecipientRateLimitedException;
import com.example.homework.exception.UnsupportedNotificationTypeException;
import com.example.homework.handler.NotificationHandler;
import com.example.homework.model.NotificationType;
import com.example.homework.repository.NotificationRepository;
import com.example.homework.util.TargetMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

  private final NotificationRepository repository;
  private final RecipientPolicy recipientPolicy;
  private final NotificationCircuitBreaker circuitBreaker;
  private final TransactionTemplate transactionTemplate;
  private final Map<NotificationType, NotificationHandler> handlers = new EnumMap<>(NotificationType.class);

  public NotificationService(
      NotificationRepository repository,
      List<NotificationHandler> handlers,
      RecipientPolicy recipientPolicy,
      NotificationCircuitBreaker circuitBreaker,
      TransactionTemplate transactionTemplate) {
    this.repository = repository;
    this.recipientPolicy = recipientPolicy;
    this.circuitBreaker = circuitBreaker;
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
    if (dispatch.rejection() != null) {
      log.warn("Dispatch {} rejected: {}", dispatch.id(), dispatch.rejection().getMessage());
      throw dispatch.rejection();
    }
    if (dispatch.circuitOpen()) {
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
      log.info("Dispatch {} sent ({} to {})", dispatch.id(), safeType, TargetMasker.mask(safeTarget));
      return repository.getDispatchById(dispatch.id());
    } catch (Exception e) {
      repository.recordDatabaseFailure(dispatch.id(), e.getMessage());
      log.warn("Dispatch {} failed ({} to {})", dispatch.id(), safeType, TargetMasker.mask(safeTarget), e);
      throw new NotificationDeliveryException(e.getMessage(), e);
    }
  }

  /**
   * Edits the stored record of a finished notification. Nothing is sent again, and the status,
   * counters and failure message are left as they were.
   */
  public NotificationResponse updateNotification(long id, NotificationUpdateRequest request) {
    String target = request.target() != null ? request.target().trim() : null;
    if (target != null && target.isEmpty()) {
      throw new InvalidNotificationRequestException("Target must not be blank.");
    }
    if (target == null && request.payload() == null) {
      throw new InvalidNotificationRequestException("Nothing to update: provide a target or a payload.");
    }

    if (!repository.updateDetails(id, target, request.payload())) {
      NotificationResponse existing = repository.findById(id)
          .orElseThrow(() -> new NotificationNotFoundException("Notification " + id + " not found."));
      throw new NotificationNotUpdatableException(
          "Notification " + id + " cannot be updated while its status is " + existing.getStatus() + ".");
    }

    log.info("Notification {} updated", id);
    return repository.getDispatchById(id);
  }

  public List<NotificationResponse> getHistory() {
    return repository.getHistory();
  }

  public NotificationResponse getLatestState() {
    return repository.getLatestState();
  }

  /**
   * Runs inside a transaction: records the new dispatch, enforces the recipient policy and samples
   * the breaker. A refused recipient is still recorded (as rejected and blocked) for auditing.
   */
  private Dispatch admit(String type, String target, String payload) {
    try {
      recipientPolicy.enforce(type, target);
    } catch (RecipientBlockedException | RecipientRateLimitedException e) {
      long id = repository.openDispatch(type, target, payload);
      repository.markAsBlocked(id, e.getMessage());
      return new Dispatch(id, false, e);
    }
    long id = repository.openDispatch(type, target, payload);
    return new Dispatch(id, circuitBreaker.isOpen(), null);
  }

  private record Dispatch(long id, boolean circuitOpen, NotificationException rejection) {
  }
}
