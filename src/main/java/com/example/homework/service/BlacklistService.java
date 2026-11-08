package com.example.homework.service;

import java.util.List;

import com.example.homework.dto.BlacklistEntryResponse;
import com.example.homework.exception.BlacklistEntryNotFoundException;
import com.example.homework.repository.BlacklistRepository;
import com.example.homework.util.TargetMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BlacklistService {

  private static final Logger log = LoggerFactory.getLogger(BlacklistService.class);

  private final BlacklistRepository repository;

  public BlacklistService(BlacklistRepository repository) {
    this.repository = repository;
  }

  public BlacklistEntryResponse add(String target, String reason) {
    BlacklistEntryResponse entry = repository.add(target, reason);
    log.info("Blacklisted recipient {} (entry {})", TargetMasker.mask(entry.target()), entry.id());
    return entry;
  }

  public List<BlacklistEntryResponse> list() {
    return repository.findAll();
  }

  public void remove(long id) {
    if (!repository.deleteById(id)) {
      throw new BlacklistEntryNotFoundException("Blacklist entry " + id + " not found.");
    }
    log.info("Removed blacklist entry {}", id);
  }
}
