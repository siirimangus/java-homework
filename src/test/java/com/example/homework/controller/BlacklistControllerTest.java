package com.example.homework.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.homework.service.BlacklistService;
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
class BlacklistControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private BlacklistService blacklistService;

  @Test
  void addedRecipientIsStoredTrimmedAndListed() throws Exception {
    add("{\"target\":\"  Spam@Example.com \",\"reason\":\"complaints\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.target").value("Spam@Example.com"))
        .andExpect(jsonPath("$.reason").value("complaints"));

    mockMvc.perform(get("/api/blacklist"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].target").value("Spam@Example.com"));
  }

  @Test
  void addingTheSameRecipientTwiceReturnsConflict() throws Exception {
    add("{\"target\":\"spam@example.com\"}").andExpect(status().isCreated());

    add("{\"target\":\" spam@example.com \"}").andExpect(status().isConflict());
  }

  @Test
  void blankTargetReturnsBadRequest() throws Exception {
    add("{\"target\":\"  \"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.detail").value("Target is mandatory."));
  }

  @Test
  void blacklistedRecipientIsRejectedUntilRemoved() throws Exception {
    long id = blacklistService.add("Spam@Example.com", "complaints").id();

    send("EMAIL", "spam@example.com").andExpect(status().isForbidden());

    mockMvc.perform(delete("/api/blacklist/" + id)).andExpect(status().isNoContent());

    send("EMAIL", "spam@example.com").andExpect(status().isOk());
  }

  @Test
  void blacklistMatchesEmailsIgnoringCaseButOtherTargetsExactly() throws Exception {
    blacklistService.add("Spam@Example.com", "complaints");
    blacklistService.add("DeviceToken-AbC", "abuse");

    send("EMAIL", "SPAM@example.COM").andExpect(status().isForbidden());
    send("PUSH", "DeviceToken-AbC").andExpect(status().isForbidden());
    send("PUSH", "devicetoken-abc").andExpect(status().isOk());
  }

  @Test
  void removingUnknownEntryReturnsNotFound() throws Exception {
    mockMvc.perform(delete("/api/blacklist/999")).andExpect(status().isNotFound());
  }

  private ResultActions add(String body) throws Exception {
    return mockMvc.perform(post("/api/blacklist")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));
  }

  private ResultActions send(String type, String target) throws Exception {
    return mockMvc.perform(post("/api/notifications/send")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"type\":\"" + type + "\",\"target\":\"" + target + "\",\"payload\":\"hello\"}"));
  }
}
