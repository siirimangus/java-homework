package com.example.homework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.example.homework.dto.NotificationResponse;
import com.example.homework.exception.CircuitBreakerOpenException;
import com.example.homework.exception.NotificationDeliveryException;
import com.example.homework.model.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationCircuitBreakerIntegrationTest {

  private static final Duration COOLDOWN_PASSED = Duration.ofSeconds(31);

  @Autowired
  private NotificationService service;

  @Autowired
  private MutableClock clock;

  @Test
  void repeatedFailuresRejectFurtherDispatches() {
    failThreeTimes();

    assertThatThrownBy(() -> service.processNotification("SMS", "ok-user@example.com", "hello"))
        .isInstanceOf(CircuitBreakerOpenException.class);
  }

  @Test
  void rejectedAttemptsDoNotKeepTheBreakerOpenAfterTheCooldown() {
    failThreeTimes();
    assertThatThrownBy(() -> service.processNotification("SMS", "ok-user@example.com", "hello"))
        .isInstanceOf(CircuitBreakerOpenException.class);

    clock.advance(COOLDOWN_PASSED);

    NotificationResponse trial = service.processNotification("SMS", "ok-user@example.com", "hello");
    assertThat(trial.status()).isEqualTo(NotificationStatus.SENT);
    assertThat(trial.failureCount()).isZero();

    NotificationResponse next = service.processNotification("PUSH", "other-user@example.com", "hello");
    assertThat(next.status()).isEqualTo(NotificationStatus.SENT);
  }

  @Test
  void failedTrialDispatchOpensTheBreakerAgain() {
    failThreeTimes();
    clock.advance(COOLDOWN_PASSED);

    assertThatThrownBy(() -> service.processNotification("EMAIL", "error-trial@example.com", "boom"))
        .isInstanceOf(NotificationDeliveryException.class);

    assertThatThrownBy(() -> service.processNotification("SMS", "ok-user@example.com", "hello"))
        .isInstanceOf(CircuitBreakerOpenException.class);
  }

  private void failThreeTimes() {
    for (int i = 0; i < 3; i++) {
      assertThatThrownBy(() -> service.processNotification("EMAIL", "error-user@example.com", "boom"))
          .isInstanceOf(NotificationDeliveryException.class);
    }
  }

  @TestConfiguration
  static class ClockTestConfig {

    @Bean
    @Primary
    MutableClock mutableClock() {
      return new MutableClock();
    }
  }

  /** A clock the test can move forward, so the cooldown can be tested without sleeping. */
  static class MutableClock extends Clock {

    private volatile Instant now = Instant.now();

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
