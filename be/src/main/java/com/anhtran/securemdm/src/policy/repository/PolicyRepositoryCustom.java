package com.anhtran.securemdm.src.policy.repository;

import com.anhtran.securemdm.src.policy.entity.Policy;
import com.anhtran.securemdm.src.policy.repository.projection.PolicyView;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PolicyRepositoryCustom {
  Mono<Policy> lock(String organizationId, String id);

  Flux<PolicyView> findLatestVersions(String organizationId, int size, long offset);
}
