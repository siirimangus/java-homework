package com.example.homework.controller;

import java.util.List;

import com.example.homework.dto.BlacklistEntryResponse;
import com.example.homework.dto.BlacklistRequest;
import com.example.homework.service.BlacklistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blacklist")
public class BlacklistController {

  private final BlacklistService service;

  public BlacklistController(BlacklistService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BlacklistEntryResponse add(@Valid @RequestBody BlacklistRequest request) {
    return service.add(request);
  }

  @GetMapping
  public List<BlacklistEntryResponse> list() {
    return service.list();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable long id) {
    service.remove(id);
  }
}
