package com.anhtran.securemdm.src.command.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommandResponse {
  private String id;
  private String organizationId;
  private String deviceId;
  private String createdBy;
  private String type;
  private Map<String, Object> payload;
  private String status;
  private Integer attempts;
  private String currentAttemptId;
  private Long leaseUntil;
  private Long createdAt;
  private Long expiresAt;
  private Long completedAt;
  private Map<String, Object> result;
}
