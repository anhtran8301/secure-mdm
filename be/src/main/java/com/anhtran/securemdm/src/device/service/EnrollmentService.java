package com.anhtran.securemdm.src.device.service;

import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.device.dto.request.EnrollDeviceRequest;
import com.anhtran.securemdm.src.device.dto.response.DeviceEnrollmentResponse;
import com.anhtran.securemdm.src.device.dto.response.EnrollmentTokenResponse;
import reactor.core.publisher.Mono;

public interface EnrollmentService {
  Mono<EnrollmentTokenResponse> issue(Actor actor);

  Mono<Void> revoke(Actor actor, String tokenId);

  /** Atomically consumes a one-time token, creates the device and its credential. */
  Mono<DeviceEnrollmentResponse> enroll(EnrollDeviceRequest request);
}
