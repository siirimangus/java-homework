package com.example.homework.controller;

import java.util.stream.Collectors;

import com.example.homework.exception.CircuitBreakerOpenException;
import com.example.homework.exception.InvalidNotificationRequestException;
import com.example.homework.exception.NotificationDeliveryException;
import com.example.homework.exception.NotificationNotFoundException;
import com.example.homework.exception.NotificationNotUpdatableException;
import com.example.homework.exception.RecipientBlockedException;
import com.example.homework.exception.UnsupportedNotificationTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps exceptions to RFC 7807 problem responses. Standard Spring MVC errors (malformed JSON, wrong
 * method, unknown path, ...) are handled by the base class; service exceptions are mapped here.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String detail = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .sorted()
        .collect(Collectors.joining(" "));
    ex.getBody().setDetail(detail);
    return super.handleMethodArgumentNotValid(ex, headers, status, request);
  }

  @ExceptionHandler({InvalidNotificationRequestException.class, UnsupportedNotificationTypeException.class})
  public ProblemDetail handleBadRequest(RuntimeException ex) {
    return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(NotificationNotFoundException.class)
  public ProblemDetail handleNotFound(NotificationNotFoundException ex) {
    return problem(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(NotificationNotUpdatableException.class)
  public ProblemDetail handleNotUpdatable(NotificationNotUpdatableException ex) {
    return problem(HttpStatus.CONFLICT, ex.getMessage());
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
