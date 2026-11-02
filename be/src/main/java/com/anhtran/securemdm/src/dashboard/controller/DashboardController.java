package com.anhtran.securemdm.src.dashboard.controller;

import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.dashboard.dto.response.OverviewResponse;
import com.anhtran.securemdm.src.dashboard.service.DashboardService;
import com.anhtran.securemdm.utils.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_PREFIX + "/dashboard")
public class DashboardController {
  private final DashboardService dashboardService;

  @GetMapping("/overview")
  public Mono<OverviewResponse> overview(@AuthenticationPrincipal Actor actor) {
    return dashboardService.overview(actor);
  }
}
