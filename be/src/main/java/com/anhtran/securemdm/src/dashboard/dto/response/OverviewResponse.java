package com.anhtran.securemdm.src.dashboard.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewResponse {
  private DeviceStatsResponse devices;
  private List<CommandStatusCountResponse> commands;
}
