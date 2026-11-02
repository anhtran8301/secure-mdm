package com.anhtran.securemdm.src.device.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentTokenResponse {
  private String id;
  private String token;
  private Long expiresAt;
}
