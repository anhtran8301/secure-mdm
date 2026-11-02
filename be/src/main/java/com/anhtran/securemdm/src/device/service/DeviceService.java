package com.anhtran.securemdm.src.device.service;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.device.dto.model.Inventory;
import com.anhtran.securemdm.src.device.dto.response.DeviceResponse;
import reactor.core.publisher.Mono;

public interface DeviceService {
  Mono<Void> heartbeat(Actor device, Inventory inventory);

  /** Revokes credentials and cancels or marks UNKNOWN the device's open commands. */
  Mono<Void> revoke(Actor actor, String deviceId);

  Mono<DeviceResponse> get(Actor actor, String deviceId);

  Mono<PageResponse<DeviceResponse>> list(Actor actor, String search, int page, int size);
}
