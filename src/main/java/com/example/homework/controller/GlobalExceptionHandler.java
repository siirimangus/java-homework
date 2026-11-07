package com.example.homework.controller;

import com.example.homework.exception.CircuitBreakerOpenException;
import com.example.homework.exception.InvalidNotificationRequestException;
import com.example.homework.exception.NotificationDeliveryException;
import com.example.homework.exception.RecipientBlockedException;
import com.example.homework.exception.UnsupportedNotificationTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps service exceptions to RFC 7807 problem responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler({InvalidNotificationRequestException.class, UnsupportedNotificationTypeException.class})
  public ProblemDetail handleBadRequest(RuntimeException ex) {
    return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(RecipientBlockedException.class)
  public ProblemDetail handleRecipientBlocked(RecipientBlockedException ex) {
    return problem(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(CircuitBreakerOpenException.class)
  public ProblemDetail handleCircuitBreakerOpen(CircuitBreakerOpenException ex) {
    return problem(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
  }

  @ExceptionHandler(NotificationDeliveryException.class)
  public ProblemDetail handleDeliveryFailure(NotificationDeliveryException ex) {
    log.warn("Notification delivery failed", ex);
    return problem(HttpStatus.BAD_GATEWAY, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) {
    log.error("Unexpected error", ex);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error.");
  }

  private static ProblemDetail problem(HttpStatus status, String detail) {
    return ProblemDetail.forStatusAndDetail(status, detail);
  }
}
