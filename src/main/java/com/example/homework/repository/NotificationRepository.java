package com.example.homework.repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.example.homework.dto.NotificationResponse;
import com.example.homework.mapper.NotificationRowMapper;
import com.example.homework.model.DeliveryOutcome;
import com.example.homework.model.NotificationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository {

  private final JdbcTemplate jdbcTemplate;
  private final NotificationRowMapper rowMapper;
  private final Clock clock;

  public NotificationRepository(JdbcTemplate jdbcTemplate, NotificationRowMapper rowMapper, Clock clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.rowMapper = rowMapper;
    this.clock = clock;
  }

  public boolean isUserBlacklistedOrRateLimited(String target) {
    if (target != null && target.toLowerCase().contains("blocked")) {
      return true;
    }

    Integer recentCount = jdbcTemplate.queryForObject(
        "select count(*) "
            + "from ntf_tx "
            + "where tgt = ? "
            + "  and crt >= dateadd('minute', -1, current_timestamp)",
        Integer.class,
        target);

    return recentCount != null && recentCount >= 3;
  }

  /**
   * Inserts a new dispatch row and returns its generated id. The failure and dispatch counters are
   * derived inside the single insert statement, so no read-then-write happens in application code.
   */
  public long openDispatch(String type, String target, String payload) {
    String blocked = (target != null && target.toLowerCase().contains("blocked")) ? "Y" : "N";
    Timestamp now = Timestamp.from(clock.instant());
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(connection -> {
      PreparedStatement statement = connection.prepareStatement(
          "insert into ntf_tx(tp, tgt, pld, st, fc, dc, bl, lm, crt, upd) "
              + "values (?, ?, ?, ?, "
              + "coalesce((select fc from ntf_tx order by id desc limit 1), 0), "
              + "(select coalesce(max(dc), 0) + 1 from ntf_tx), "
              + "?, ?, ?, ?)",
          new String[] {"id"});
      statement.setString(1, type);
      statement.setString(2, target);
      statement.setString(3, payload);
      statement.setString(4, NotificationStatus.NEW.getCode());
      statement.setString(5, blocked);
      statement.setString(6, "START");
      statement.setTimestamp(7, now);
      statement.setTimestamp(8, now);
      return statement;
    }, keyHolder);

    return Objects.requireNonNull(keyHolder.getKey(), "No generated id returned").longValue();
  }

  /** The latest delivery that finished as sent or failed; rejected and unsupported rows are ignored. */
  public Optional<DeliveryOutcome> getLastDeliveryOutcome() {
    return jdbcTemplate.query(
            "select fc, upd from ntf_tx where st in (?, ?) order by id desc limit 1",
            (rs, rowNum) -> new DeliveryOutcome(rs.getInt("fc"), rs.getTimestamp("upd").toInstant()),
            NotificationStatus.SENT.getCode(),
            NotificationStatus.ERROR.getCode())
        .stream()
        .findFirst();
  }

  public void recordDatabaseFailure(long id, String error) {
    jdbcTemplate.update(
        "update ntf_tx set st = ?, fc = fc + 1, lm = ?, upd = ? where id = ?",
        NotificationStatus.ERROR.getCode(), shorten(error), Timestamp.from(clock.instant()), id);
  }

  public void resetDatabaseFailures(long id) {
    jdbcTemplate.update(
        "update ntf_tx set st = ?, fc = ?, lm = ?, upd = ? where id = ?",
        NotificationStatus.SENT.getCode(), 0, "SENT", Timestamp.from(clock.instant()), id);
  }

  public void recordUnsupportedType(long id, String type) {
    jdbcTemplate.update(
        "update ntf_tx set st = ?, lm = ?, upd = ? where id = ?",
        NotificationStatus.UNKNOWN.getCode(), shorten("Unsupported type: " + type), Timestamp.from(clock.instant()), id);
  }

  public void markAsSkipped(long id, String message) {
    jdbcTemplate.update(
        "update ntf_tx set st = ?, lm = ?, upd = ? where id = ?",
        NotificationStatus.REJECTED.getCode(), shorten(message), Timestamp.from(clock.instant()), id);
  }

  public NotificationResponse getLatestState() {
    List<NotificationResponse> items = jdbcTemplate.query(
        "select * from ntf_tx order by id desc limit 1",
        rowMapper);
    return items.isEmpty() ? null : items.get(0);
  }

  public Optional<NotificationResponse> findById(long id) {
    return jdbcTemplate.query("select * from ntf_tx where id = ?", rowMapper, id).stream().findFirst();
  }

  /**
   * Changes the given fields (null leaves a field unchanged) of a finished notification. The
   * status condition is part of the statement, so the check and the change are atomic.
   *
   * @return whether a notification was updated
   */
  public boolean updateDetails(long id, String target, String payload) {
    int updated = jdbcTemplate.update(
        "update ntf_tx set tgt = coalesce(?, tgt), pld = coalesce(?, pld), upd = ? "
            + "where id = ? and st in (?, ?)",
        target, payload, Timestamp.from(clock.instant()), id,
        NotificationStatus.SENT.getCode(), NotificationStatus.ERROR.getCode());
    return updated > 0;
  }

  public NotificationResponse getDispatchById(long id) {
    return jdbcTemplate.queryForObject("select * from ntf_tx where id = ?", rowMapper, id);
  }

  public List<NotificationResponse> getHistory() {
    return jdbcTemplate.query("select * from ntf_tx order by id desc limit 20", rowMapper);
  }

  private String shorten(String value) {
    if (value == null || value.length() <= 255) {
      return value;
    }
    return value.substring(0, 255);
  }
}
