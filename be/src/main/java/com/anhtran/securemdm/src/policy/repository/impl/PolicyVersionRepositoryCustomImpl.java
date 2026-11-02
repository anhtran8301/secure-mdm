package com.anhtran.securemdm.src.policy.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.policy.entity.PolicyVersion;
import com.anhtran.securemdm.src.policy.repository.PolicyVersionRepositoryCustom;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class PolicyVersionRepositoryCustomImpl extends SqlSupport
    implements PolicyVersionRepositoryCustom {
  public PolicyVersionRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Mono<Integer> maxVersion(String organizationId, String policyId) {
    return count(
            "SELECT CAST(COALESCE(MAX(version),0) AS SIGNED) FROM policy_versions WHERE"
                + " organization_id=? AND policy_id=?",
            organizationId,
            policyId)
        .map(Long::intValue);
  }

  @Override
  public Flux<PolicyVersion> findPage(
      String organizationId, String policyId, int size, long offset) {
    return all(
        PolicyVersion.class,
        "SELECT * FROM policy_versions WHERE organization_id=? AND policy_id=? ORDER BY version"
            + " DESC LIMIT ? OFFSET ?",
        organizationId,
        policyId,
        size,
        offset);
  }
}
