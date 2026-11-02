package com.anhtran.securemdm.src.device.controller;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.device.dto.response.DeviceResponse;
import com.anhtran.securemdm.src.device.service.DeviceService;
import com.anhtran.securemdm.utils.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_PREFIX + "/devices")
public class DeviceController {
  private final DeviceService deviceService;

  @GetMapping
  public Mono<PageResponse<DeviceResponse>> list(
      @AuthenticationPrincipal Actor actor,
      @RequestParam(defaultValue = "") String search,
      @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
      @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
    return deviceService.list(actor, search, page, size);
  }

  @GetMapping("/{id}")
  public Mono<DeviceResponse> get(@AuthenticationPrincipal Actor actor, @PathVariable String id) {
    return deviceService.get(actor, id);
  }

  @PostMapping("/{id}/revoke")
  public Mono<Void> revoke(@AuthenticationPrincipal Actor actor, @PathVariable String id) {
    return deviceService.revoke(actor, id);
  }
}
