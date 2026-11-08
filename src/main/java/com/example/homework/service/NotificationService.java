package com.example.homework.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.homework.dto.NotificationResponse;
import com.example.homework.dto.NotificationUpdateRequest;
import com.example.homework.exception.CircuitBreakerOpenException;
import com.example.homework.exception.ConflictException;
import com.example.homework.exception.InvalidNotificationRequestException;
import com.example.homework.exception.NotificationDeliveryException;
import com.example.homework.exception.NotificationException;
import com.example.homework.exception.RecipientBlockedException;
import com.example.homework.exception.RecipientRateLimitedException;
import com.example.homework.exception.ResourceNotFoundException;
import com.example.homework.exception.UnsupportedNotificationTypeException;
import com.example.homework.handler.NotificationHandler;
import com.example.homework.model.NotificationStatus;
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
  private final Map<NotificationType, NotificationHandler> handlers;

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
    this.handlers = handlers.stream()
        .collect(Collectors.toMap(NotificationHandler::getType, Function.identity()));
  }

  /**
   * Dispatches a notification. The request is admitted in one short transaction, delivery runs
   * outside any transaction (so slow channels do not hold a database connection or a lock), and
   * the outcome is recorded with a single atomic statement.
   */
  public NotificationResponse processNotification(String type, String target, String payload) {
    String safeType = type != null ? type.trim().toUpperCase() : "";
    String safeTarget = target != null ? target.trim() : "";
    String safePayload = payload != null ? payload : "";

    if (safeTarget.isEmpty()) {
      throw new InvalidNotificationRequestException("Target is mandatory.");
    }

    Optional<NotificationHandler> handler = NotificationType.find(safeType).map(handlers::get);
    Dispatch dispatch = transactionTemplate.execute(
        status -> admit(safeType, safeTarget, safePayload, handler.isPresent()));

    // Rejections are thrown after the admission transaction has committed so the row is kept.
    if (dispatch.rejection() != null) {
      log.warn("Dispatch {} rejected: {}", dispatch.id(), dispatch.rejection().getMessage());
      throw dispatch.rejection();
    }

    try {
      handler.orElseThrow().execute(safeTarget, safePayload);
    } catch (Exception e) {
      repository.markAsFailed(dispatch.id(), e.getMessage());
      log.warn("Dispatch {} failed ({} to {})", dispatch.id(), safeType, TargetMasker.mask(safeTarget), e);
      throw new NotificationDeliveryException(e.getMessage(), e);
    }
    repository.markAsSent(dispatch.id());
    log.info("Dispatch {} sent ({} to {})", dispatch.id(), safeType, TargetMasker.mask(safeTarget));
    return findOrThrow(dispatch.id());
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
      NotificationResponse existing = findOrThrow(id);
      throw new ConflictException(
          "Notification " + id + " cannot be updated while its status is " + existing.status() + ".");
    }

    log.info("Notification {} updated", id);
    return findOrThrow(id);
  }

  public List<NotificationResponse> getHistory() {
    return repository.getHistory();
  }

  public NotificationResponse getLatestState() {
    return repository.getLatestState();
  }

  /**
   * Runs inside a transaction: records the attempt and decides whether it may be delivered. Every
   * refusal is stored on the row (the transaction must not roll back) and returned as the rejection.
   */
  private Dispatch admit(String type, String target, String payload, boolean supported) {
    try {
      recipientPolicy.enforce(type, target);
    } catch (RecipientBlockedException | RecipientRateLimitedException e) {
      long id = repository.openDispatch(type, target, payload);
      repository.markAsBlocked(id, e.getMessage());
      return new Dispatch(id, e);
    }

    long id = repository.openDispatch(type, target, payload);
    if (circuitBreaker.isOpen()) {
      String message = "Circuit breaker OPEN via database state.";
      repository.updateStatus(id, NotificationStatus.REJECTED, message);
      return new Dispatch(id, new CircuitBreakerOpenException(message));
    }
    if (!supported) {
      String message = "Unsupported type: " + type;
      repository.updateStatus(id, NotificationStatus.UNKNOWN, message);
      return new Dispatch(id, new UnsupportedNotificationTypeException(message));
    }
    return new Dispatch(id, null);
  }

  private NotificationResponse findOrThrow(long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Notification " + id + " not found."));
  }

  private record Dispatch(long id, NotificationException rejection) {
  }
}
