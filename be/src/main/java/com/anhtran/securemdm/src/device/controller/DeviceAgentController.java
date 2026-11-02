package com.anhtran.securemdm.src.device.controller;

import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.device.dto.model.Inventory;
import com.anhtran.securemdm.src.device.service.DeviceService;
import com.anhtran.securemdm.utils.AppConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Endpoints called by an enrolled device; the principal is the device itself (role DEVICE). */
@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_PREFIX + "/device-agent")
public class DeviceAgentController {
  private final DeviceService deviceService;

  @PostMapping("/heartbeat")
  public Mono<Void> heartbeat(
      @AuthenticationPrincipal Actor device, @Valid @RequestBody Inventory inventory) {
    return deviceService.heartbeat(device, inventory);
  }
}
