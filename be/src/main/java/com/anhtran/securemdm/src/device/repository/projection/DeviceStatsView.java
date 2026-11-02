package com.anhtran.securemdm.src.device.repository.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Dashboard device counters. */
@Getter
@Setter
@NoArgsConstructor
public class DeviceStatsView {
  private Long total;
  private Long online;
  private Long revoked;
}
