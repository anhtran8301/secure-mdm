package com.anhtran.securemdm.src.device.repository;

import com.anhtran.securemdm.src.device.entity.DeviceCertificate;
import reactor.core.publisher.Mono;

public interface DeviceCertificateRepositoryCustom {
  /** Unrevoked certificate with {@code notBefore <= now < notAfter} whose device is ACTIVE. */
  Mono<DeviceCertificate> findUsable(String serial, long now);

  /** The device's newest certificate, revoked or not. */
  Mono<DeviceCertificate> findLatest(String organizationId, String deviceId);

  Mono<Long> revokeForDevice(String organizationId, String deviceId, long at);
}
