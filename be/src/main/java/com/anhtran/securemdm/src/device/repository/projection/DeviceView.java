package com.anhtran.securemdm.src.device.repository.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Device row plus connectivity/compliance derived from heartbeat and policy state. */
@Getter
@Setter
@NoArgsConstructor
public class DeviceView {
  private String id;
  private String organizationId;
  private String deviceUid;
  private String name;
  private String platform;
  private String status;
  private Long lastSeenAt;
  private Long createdAt;
  private String inventoryJson;
  private Long assignmentRevision;
  private String connectivity;
  private String compliance;
}
