package com.anhtran.securemdm.src.dashboard.service.impl;

import com.anhtran.securemdm.config.MdmProperties;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.command.repository.DeviceCommandRepository;
import com.anhtran.securemdm.src.dashboard.dto.response.OverviewResponse;
import com.anhtran.securemdm.src.dashboard.mapper.DashboardMapper;
import com.anhtran.securemdm.src.dashboard.service.DashboardService;
import com.anhtran.securemdm.src.device.repository.DeviceRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
  private final DeviceRepository deviceRepository;
  private final DeviceCommandRepository commandRepository;
  private final DashboardMapper dashboardMapper;
  private final MdmProperties properties;
  private final Clock clock;

  @Override
  public Mono<OverviewResponse> overview(Actor actor) {
    String org = actor.organizationId();
    return Mono.zip(
            deviceRepository
                .stats(org, clock.millis() - properties.offlineMs())
                .map(dashboardMapper::toResponse),
            commandRepository.countByStatus(org).map(dashboardMapper::toResponse).collectList())
        .map(p -> new OverviewResponse(p.getT1(), p.getT2()));
  }
}
