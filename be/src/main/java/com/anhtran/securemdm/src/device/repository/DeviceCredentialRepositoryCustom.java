package com.anhtran.securemdm.src.device.repository;

import com.anhtran.securemdm.src.device.entity.Device;
import reactor.core.publisher.Mono;

public interface DeviceCredentialRepositoryCustom {
  /** Device (id, organizationId only) owning an active credential with this secret hash. */
  Mono<Device> findActiveDevice(String secretHash, long now);

  Mono<Long> revokeForDevice(String organizationId, String deviceId, long at);
}
