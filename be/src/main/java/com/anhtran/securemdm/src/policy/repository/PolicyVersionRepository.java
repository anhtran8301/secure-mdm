package com.anhtran.securemdm.src.policy.repository;

import com.anhtran.securemdm.src.policy.entity.PolicyVersion;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PolicyVersionRepository
    extends ReactiveCrudRepository<PolicyVersion, String>, PolicyVersionRepositoryCustom {
  Mono<PolicyVersion> findByOrganizationIdAndId(String organizationId, String id);

  Mono<Long> countByOrganizationIdAndPolicyId(String organizationId, String policyId);
}
