package com.example.homework.repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.example.homework.dto.NotificationResponse;
import com.example.homework.model.DeliveryOutcome;
import com.example.homework.model.NotificationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository {

  private static final RowMapper<NotificationResponse> ROW_MAPPER = (rs, rowNum) ->
      new NotificationResponse(
          rs.getLong("id"),
          rs.getString("tp"),
          rs.getString("tgt"),
          rs.getString("pld"),
          NotificationStatus.fromCode(rs.getString("st")),
          rs.getInt("fc"),
          rs.getInt("dc"),
          "Y".equals(rs.getString("bl")),
          rs.getString("lm"),
          rs.getTimestamp("crt").toInstant(),
          rs.getTimestamp("upd").toInstant());

  private final JdbcTemplate jdbcTemplate;
  private final Clock clock;

  public NotificationRepository(JdbcTemplate jdbcTemplate, Clock clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.clock = clock;
  }

  /** Dispatches accepted for the recipient since the given instant; rejected rows are not counted. */
  public int countDispatchesSince(String target, boolean ignoreCase, Instant since) {
    String condition = ignoreCase ? "lower(tgt) = lower(?)" : "tgt = ?";
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from ntf_tx where " + condition + " and crt >= ? and st <> ?",
        Integer.class,
        target,
        Timestamp.from(since),
        NotificationStatus.REJECTED.getCode());
    return count == null ? 0 : count;
  }

  /**
   * Inserts a new dispatch row and returns its generated id. The failure and dispatch counters are
   * derived inside the single insert statement, so no read-then-write happens in application code.
   */
  public long openDispatch(String type, String target, String payload) {
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
      statement.setString(5, "N");
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

  public void markAsBlocked(long id, String message) {
    jdbcTemplate.update(
        "update ntf_tx set st = ?, bl = ?, lm = ?, upd = ? where id = ?",
        NotificationStatus.REJECTED.getCode(), "Y", shorten(message), Timestamp.from(clock.instant()), id);
  }

  public NotificationResponse getLatestState() {
    return jdbcTemplate.query("select * from ntf_tx order by id desc limit 1", ROW_MAPPER)
        .stream()
        .findFirst()
        .orElse(null);
  }

  public Optional<NotificationResponse> findById(long id) {
    return jdbcTemplate.query("select * from ntf_tx where id = ?", ROW_MAPPER, id).stream().findFirst();
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
    return jdbcTemplate.queryForObject("select * from ntf_tx where id = ?", ROW_MAPPER, id);
  }

  public List<NotificationResponse> getHistory() {
    return jdbcTemplate.query("select * from ntf_tx order by id desc limit 20", ROW_MAPPER);
  }

  private String shorten(String value) {
    if (value == null || value.length() <= 255) {
      return value;
    }
    return value.substring(0, 255);
  }
}
