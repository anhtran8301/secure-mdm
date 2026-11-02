package com.anhtran.securemdm.src.device.dto.response;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {
  private String id;
  private String organizationId;
  private String deviceUid;
  private String name;
  private String platform;
  private String status;
  private Long lastSeenAt;
  private Long createdAt;
  private Map<String, Object> inventory;
  private Long assignmentRevision;
  private String connectivity;
  private String compliance;
}
