package com.anhtran.securemdm.src.device.service;

import reactor.core.publisher.Mono;

/**
 * Hook for modules that must react when a device is revoked. Implementations run inside the
 * revocation transaction, so a failure rolls the whole revocation back.
 */
public interface DeviceRevocationListener {
  Mono<Void> onDeviceRevoked(String organizationId, String deviceId);
}
