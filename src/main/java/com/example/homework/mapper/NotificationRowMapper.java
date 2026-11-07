package com.example.homework.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.homework.dto.LegacyNotificationResponse;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationRowMapper implements RowMapper<LegacyNotificationResponse> {

  @Override
  public LegacyNotificationResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
    LegacyNotificationResponse response = new LegacyNotificationResponse();
    response.setId(rs.getLong("id"));
    response.setType(rs.getString("tp"));
    response.setTarget(rs.getString("tgt"));
    response.setPayload(rs.getString("pld"));
    response.setStatus(rs.getString("st"));
    response.setFailureCount(rs.getInt("fc"));
    response.setDispatchCount(rs.getInt("dc"));
    response.setBlocked("Y".equals(rs.getString("bl")) ? "Y" : "N");
    response.setMessage(rs.getString("lm"));

    response.setCreatedAt(rs.getTimestamp("crt"));
    response.setUpdatedAt(rs.getTimestamp("upd"));
    return response;
  }
}
