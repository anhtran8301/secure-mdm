package com.anhtran.securemdm.src.device.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceEnrollmentResponse {
  private String deviceId;
  private String organizationId;
  private String credential;
  private Long expiresAt;
}
