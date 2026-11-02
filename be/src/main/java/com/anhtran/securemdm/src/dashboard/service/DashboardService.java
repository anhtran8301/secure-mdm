package com.anhtran.securemdm.src.dashboard.service;

import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.dashboard.dto.response.OverviewResponse;
import reactor.core.publisher.Mono;

public interface DashboardService {
  Mono<OverviewResponse> overview(Actor actor);
}
