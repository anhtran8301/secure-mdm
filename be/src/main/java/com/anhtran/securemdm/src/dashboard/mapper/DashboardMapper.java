package com.anhtran.securemdm.src.dashboard.mapper;

import com.anhtran.securemdm.src.command.repository.projection.CommandStatusCountView;
import com.anhtran.securemdm.src.dashboard.dto.response.CommandStatusCountResponse;
import com.anhtran.securemdm.src.dashboard.dto.response.DeviceStatsResponse;
import com.anhtran.securemdm.src.device.repository.projection.DeviceStatsView;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DashboardMapper {
  private final ModelMapper modelMapper;

  public DeviceStatsResponse toResponse(DeviceStatsView stats) {
    return modelMapper.map(stats, DeviceStatsResponse.class);
  }

  public CommandStatusCountResponse toResponse(CommandStatusCountView count) {
    return modelMapper.map(count, CommandStatusCountResponse.class);
  }
}
