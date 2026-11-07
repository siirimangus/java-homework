package com.example.homework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.example.homework.dto.NotificationResponse;
import com.example.homework.model.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NotificationServiceIntegrationTest {

  @Autowired
  private NotificationService service;

  @Test
  void emailSuccessIsStoredAsSent() throws Exception {
    NotificationResponse response = service.processNotification("EMAIL", "buyer@example.com", "hello");

    assertThat(response.getStatus()).isEqualTo(NotificationStatus.SENT);
    assertThat(response.getDispatchCount()).isEqualTo(1);
    assertThat(response.getFailureCount()).isZero();
    assertThat(service.getHistory()).hasSize(1);
  }

  @Test
  void blockedTargetFailsBeforeDispatchIsInserted() {
    try {
      service.processNotification("SMS", "blocked-user@example.com", "hello");
      fail("Expected blocked target to fail");
    } catch (RuntimeException ex) {
      assertThat(ex.getMessage()).isEqualTo("Business Rule Violation: User is blacklisted or rate-limited by DB policy.");
    } catch (Exception ex) {
      fail("Expected RuntimeException but got: " + ex.getClass().getName());
    }

    assertThat(service.getHistory()).isEmpty();
  }

  @Test
  void unsupportedTypeIsStoredAsUnknown() {
    try {
      service.processNotification("FAX", "ops@example.com", "hello");
      fail("Expected unsupported type to fail");
    } catch (Exception ex) {
      assertThat(ex.getMessage()).isEqualTo("Unsupported type: FAX");
    }

    NotificationResponse latest = service.getLatestState();
    assertThat(latest.getStatus()).isEqualTo(NotificationStatus.UNKNOWN);
    assertThat(latest.getMessage()).isEqualTo("Unsupported type: FAX");
  }

  @Test
  void threeFailuresOpenCircuitBreakerForNextAttempt() {
    for (int i = 0; i < 3; i++) {
      try {
        service.processNotification("EMAIL", "error-user@example.com", "boom");
        fail("Expected email failure");
      } catch (RuntimeException ex) {
        assertThat(ex.getMessage()).isEqualTo("SMTP Timeout");
      } catch (Exception ex) {
        fail("Expected RuntimeException but got: " + ex.getClass().getName());
      }
    }

    try {
      service.processNotification("SMS", "ok-user@example.com", "hello");
      fail("Expected circuit breaker to reject the request");
    } catch (RuntimeException ex) {
      assertThat(ex.getMessage()).isEqualTo("Circuit breaker OPEN via database state.");
    } catch (Exception ex) {
      fail("Expected RuntimeException but got: " + ex.getClass().getName());
    }

    NotificationResponse latest = service.getLatestState();
    assertThat(latest.getStatus()).isEqualTo(NotificationStatus.REJECTED);
    assertThat(latest.getFailureCount()).isEqualTo(3);
    assertThat(latest.getDispatchCount()).isEqualTo(4);
  }

  @Test
  void concurrentDispatchesAreAllStoredWithUniqueIds() throws Exception {
    int requests = 50;
    ExecutorService executor = Executors.newFixedThreadPool(16);
    try {
      List<Future<NotificationResponse>> futures = new ArrayList<>();
      for (int i = 0; i < requests; i++) {
        String target = "user" + i + "@example.com";
        Callable<NotificationResponse> task = () -> service.processNotification("EMAIL", target, "hello");
        futures.add(executor.submit(task));
      }

      HashSet<Long> ids = new HashSet<>();
      for (Future<NotificationResponse> future : futures) {
        NotificationResponse response = future.get();
        assertThat(response.getStatus()).isEqualTo(NotificationStatus.SENT);
        ids.add(response.getId());
      }
      assertThat(ids).hasSize(requests);
    } finally {
      executor.shutdownNow();
    }
  }
}
