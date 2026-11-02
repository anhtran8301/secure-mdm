package com.anhtran.securemdm.src.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
  private String id;
  private String organizationId;
  private String actorType;
  private String actorId;
  private String action;
  private String resourceType;
  private String resourceId;
  private String correlationId;
  private Long createdAt;
}
