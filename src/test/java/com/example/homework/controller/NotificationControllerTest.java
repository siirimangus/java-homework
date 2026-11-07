package com.example.homework.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  void successfulSendReturnsOk() throws Exception {
    send("EMAIL", "buyer@example.com")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SENT"));
  }

  @Test
  void missingTargetReturnsBadRequest() throws Exception {
    send("EMAIL", " ")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Target is mandatory."));
  }

  @Test
  void tooLongTargetReturnsBadRequest() throws Exception {
    send("EMAIL", "a".repeat(256))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Target must be at most 255 characters."));
  }

  @Test
  void malformedJsonReturnsBadRequest() throws Exception {
    mockMvc.perform(post("/api/notifications/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{not json"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unsupportedTypeReturnsBadRequest() throws Exception {
    send("FAX", "ops@example.com")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Unsupported type: FAX"));
  }

  @Test
  void blockedRecipientReturnsForbidden() throws Exception {
    send("SMS", "blocked-user@example.com")
        .andExpect(status().isForbidden());
  }

  @Test
  void deliveryFailureReturnsBadGateway() throws Exception {
    send("EMAIL", "error-user@example.com")
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.detail").value("SMTP Timeout"));
  }

  @Test
  void openCircuitBreakerReturnsServiceUnavailable() throws Exception {
    for (int i = 0; i < 3; i++) {
      send("EMAIL", "error-user@example.com").andExpect(status().isBadGateway());
    }

    send("SMS", "ok-user@example.com")
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.detail").value("Circuit breaker OPEN via database state."));
  }

  private ResultActions send(String type, String target) throws Exception {
    String body = "{\"type\":\"" + type + "\",\"target\":\"" + target + "\",\"payload\":\"hello\"}";
    return mockMvc.perform(post("/api/notifications/send")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));
  }
}
