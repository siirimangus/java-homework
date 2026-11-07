package com.example.homework.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.example.homework.dto.NotificationResponse;
import com.example.homework.model.NotificationStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
public class NotificationRowMapper implements RowMapper<NotificationResponse> {

  @Override
  public NotificationResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
    NotificationResponse response = new NotificationResponse();
    response.setId(rs.getLong("id"));
    response.setType(rs.getString("tp"));
    response.setTarget(rs.getString("tgt"));
    response.setPayload(rs.getString("pld"));
    response.setStatus(NotificationStatus.fromCode(rs.getString("st")));
    response.setFailureCount(rs.getInt("fc"));
    response.setDispatchCount(rs.getInt("dc"));
    response.setBlocked("Y".equals(rs.getString("bl")));
    response.setMessage(rs.getString("lm"));

    response.setCreatedAt(rs.getTimestamp("crt"));
    response.setUpdatedAt(rs.getTimestamp("upd"));
    return response;
  }
}
