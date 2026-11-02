package com.anhtran.securemdm.src.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceStatsResponse {
  private Long total;
  private Long online;
  private Long revoked;
}
