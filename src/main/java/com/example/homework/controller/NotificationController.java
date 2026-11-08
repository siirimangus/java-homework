package com.example.homework.controller;

import java.util.List;

import com.example.homework.dto.NotificationRequest;
import com.example.homework.dto.NotificationResponse;
import com.example.homework.dto.NotificationUpdateRequest;
import com.example.homework.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @PostMapping("/send")
  public NotificationResponse send(@Valid @RequestBody NotificationRequest request) {
    return service.processNotification(request.getType(), request.getTarget(), request.getPayload());
  }

  @PatchMapping("/{id}")
  public NotificationResponse update(
      @PathVariable long id, @Valid @RequestBody NotificationUpdateRequest request) {
    return service.updateNotification(id, request);
  }

  @GetMapping("/history")
  public List<NotificationResponse> history() {
    return service.getHistory();
  }

  @GetMapping("/latest")
  public NotificationResponse latest() {
    return service.getLatestState();
  }
}
