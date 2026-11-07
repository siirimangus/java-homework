package com.example.homework.controller;

import java.util.List;

import com.example.homework.dto.NotificationRequest;
import com.example.homework.dto.NotificationResponse;
import com.example.homework.service.NotificationService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @RequestMapping(value = "/send", method = RequestMethod.POST)
  @ResponseBody
  public NotificationResponse send(@RequestBody NotificationRequest request) {
    return service.processNotification(request.getType(), request.getTarget(), request.getPayload());
  }

  @RequestMapping(value = "/history", method = RequestMethod.GET)
  @ResponseBody
  public List<NotificationResponse> history() {
    return service.getHistory();
  }

  @RequestMapping(value = "/latest", method = RequestMethod.GET)
  @ResponseBody
  public NotificationResponse latest() {
    return service.getLatestState();
  }
}