package com.example.homework.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NotificationRequest {

  @Size(max = 32, message = "Type must be at most 32 characters.")
  private String type;

  @NotBlank(message = "Target is mandatory.")
  @Size(max = 255, message = "Target must be at most 255 characters.")
  private String target;

  private String payload;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }
}