package com.anhtran.securemdm.src.policy.repository;

import com.anhtran.securemdm.src.policy.entity.PolicyVersion;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PolicyVersionRepositoryCustom {
  Mono<Integer> maxVersion(String organizationId, String policyId);

  Flux<PolicyVersion> findPage(String organizationId, String policyId, int size, long offset);
}
