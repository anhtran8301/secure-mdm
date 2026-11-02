package com.anhtran.securemdm.src.device.repository;

import com.anhtran.securemdm.src.device.entity.EnrollmentToken;
import reactor.core.publisher.Mono;

public interface EnrollmentTokenRepositoryCustom {
  Mono<EnrollmentToken> lockByTokenHash(String tokenHash);

  Mono<Long> markUsed(String id, long at);

  Mono<Long> revoke(String organizationId, String id, long at);
}
