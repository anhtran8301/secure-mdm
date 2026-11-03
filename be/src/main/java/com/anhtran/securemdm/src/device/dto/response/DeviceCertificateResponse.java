package com.anhtran.securemdm.src.device.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The device's newest certificate, as shown on the device page. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCertificateResponse {
  private String serial;
  private String fingerprint;
  private Long notAfter;
  private boolean revoked;
}
