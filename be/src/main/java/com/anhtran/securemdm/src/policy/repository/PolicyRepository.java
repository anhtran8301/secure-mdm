package com.anhtran.securemdm.src.policy.repository;

import com.anhtran.securemdm.src.policy.entity.Policy;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PolicyRepository
    extends ReactiveCrudRepository<Policy, String>, PolicyRepositoryCustom {
  Mono<Boolean> existsByOrganizationIdAndId(String organizationId, String id);

  Mono<Long> countByOrganizationId(String organizationId);
}
