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

  /** Certificate serial (hex); devices use it as the {@code kid} of their Device Proofs. */
  private String serial;

  private String certificate;
  private String caCertificate;
  private Long expiresAt;
}
