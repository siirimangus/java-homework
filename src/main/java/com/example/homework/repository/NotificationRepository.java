package com.example.homework.repository;

import java.sql.Timestamp;
import java.util.List;

import com.example.homework.dto.NotificationResponse;
import com.example.homework.mapper.NotificationRowMapper;
import com.example.homework.model.NotificationStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepository {

  private final JdbcTemplate jdbcTemplate;
  private final NotificationRowMapper rowMapper;

  public NotificationRepository(JdbcTemplate jdbcTemplate, NotificationRowMapper rowMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.rowMapper = rowMapper;
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

  public long openDispatch(String type, String target, String payload) {
    long id = getNextId();
    int nextDispatchCount = getTotalDispatches() + 1;
    int currentFailureCount = getFailureCountFromDatabase();
    String blocked = (target != null && target.toLowerCase().contains("blocked")) ? "Y" : "N";
    Timestamp now = new Timestamp(System.currentTimeMillis());

    jdbcTemplate.update(
        "insert into ntf_tx(id, tp, tgt, pld, st, fc, dc, bl, lm, crt, upd) "
            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        id, type, target, payload, NotificationStatus.NEW.getCode(), currentFailureCount, nextDispatchCount, blocked, "START", now, now);

    return id;
  }

  public int getFailureCountFromDatabase() {
    List<Integer> results = jdbcTemplate.query(
        "select fc from ntf_tx order by id desc limit 1",
        (rs, rowNum) -> rs.getInt(1));
    return results.isEmpty() ? 0 : results.get(0);
  }

  public void recordDatabaseFailure(long id, String target, String error) {
    int nextFailureCount = getFailureCountFromDatabase() + 1;
    jdbcTemplate.update(
        "update ntf_tx set st = ?, fc = ?, lm = ?, upd = ? where id = ?",
        NotificationStatus.ERROR.getCode(), nextFailureCount, shorten(error), new Timestamp(System.currentTimeMillis()), id);
    System.out.println("[DB AUDIT] Inserted failure record for " + target + ": " + error);
  }

  public void resetDatabaseFailures(long id) {
    jdbcTemplate.update(
        "update ntf_tx set st = ?, fc = ?, lm = ?, upd = ? where id = ?",
        NotificationStatus.SENT.getCode(), 0, "SENT", new Timestamp(System.currentTimeMillis()), id);
  }

  public void recordUnsupportedType(long id, String type) {
    jdbcTemplate.update(
        "update ntf_tx set st = ?, lm = ?, upd = ? where id = ?",
        NotificationStatus.UNKNOWN.getCode(), shorten("Unsupported type: " + type), new Timestamp(System.currentTimeMillis()), id);
  }

  public void markAsSkipped(long id, String message) {
    jdbcTemplate.update(
        "update ntf_tx set st = ?, lm = ?, upd = ? where id = ?",
        NotificationStatus.REJECTED.getCode(), shorten(message), new Timestamp(System.currentTimeMillis()), id);
  }

  public NotificationResponse getLatestState() {
    List<NotificationResponse> items = jdbcTemplate.query(
        "select * from ntf_tx order by id desc limit 1",
        rowMapper);
    return items.isEmpty() ? null : items.get(0);
  }

  public NotificationResponse getDispatchById(long id) {
    return jdbcTemplate.queryForObject("select * from ntf_tx where id = ?", rowMapper, id);
  }

  public List<NotificationResponse> getHistory() {
    return jdbcTemplate.query("select * from ntf_tx order by id desc limit 20", rowMapper);
  }

  private long getNextId() {
    Long maxId = jdbcTemplate.queryForObject("select coalesce(max(id), 0) from ntf_tx", Long.class);
    return (maxId == null ? 0L : maxId) + 1L;
  }

  private int getTotalDispatches() {
    Integer latestDispatchCount = jdbcTemplate.queryForObject(
        "select coalesce(max(dc), 0) from ntf_tx",
        Integer.class);
    return latestDispatchCount == null ? 0 : latestDispatchCount;
  }

  private String shorten(String value) {
    if (value == null || value.length() <= 255) {
      return value;
    }
    return value.substring(0, 255);
  }
}
