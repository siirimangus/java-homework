package com.example.homework.repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.example.homework.dto.BlacklistEntryResponse;
import com.example.homework.exception.ConflictException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class BlacklistRepository {

  private static final RowMapper<BlacklistEntryResponse> ROW_MAPPER = (rs, rowNum) ->
      new BlacklistEntryResponse(
          rs.getLong("id"),
          rs.getString("target"),
          rs.getString("reason"),
          rs.getTimestamp("created_at").toInstant());

  private final JdbcTemplate jdbcTemplate;
  private final Clock clock;

  public BlacklistRepository(JdbcTemplate jdbcTemplate, Clock clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.clock = clock;
  }

  public boolean existsByTarget(String target, boolean ignoreCase) {
    String condition = ignoreCase ? "lower(target) = lower(?)" : "target = ?";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from ntf_blacklist where " + condition, Integer.class, target.trim());
    return count != null && count > 0;
  }

  /** The unique constraint makes a duplicate fail atomically. */
  public BlacklistEntryResponse add(String target, String reason) {
    String normalized = target.trim();
    Instant now = clock.instant();
    KeyHolder keyHolder = new GeneratedKeyHolder();

    try {
      jdbcTemplate.update(connection -> {
        PreparedStatement statement = connection.prepareStatement(
            "insert into ntf_blacklist(target, reason, created_at) values (?, ?, ?)",
            new String[] {"id"});
        statement.setString(1, normalized);
        statement.setString(2, reason);
        statement.setTimestamp(3, Timestamp.from(now));
        return statement;
      }, keyHolder);
    } catch (DuplicateKeyException e) {
      throw new ConflictException("Recipient is already blacklisted: " + normalized);
    }

    long id = Objects.requireNonNull(keyHolder.getKey(), "No generated id returned").longValue();
    return new BlacklistEntryResponse(id, normalized, reason, now);
  }

  public List<BlacklistEntryResponse> findAll() {
    return jdbcTemplate.query("select * from ntf_blacklist order by id", ROW_MAPPER);
  }

  public boolean deleteById(long id) {
    return jdbcTemplate.update("delete from ntf_blacklist where id = ?", id) > 0;
  }
}
