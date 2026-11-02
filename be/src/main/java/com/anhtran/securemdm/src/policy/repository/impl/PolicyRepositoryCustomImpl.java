package com.anhtran.securemdm.src.policy.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.policy.entity.Policy;
import com.anhtran.securemdm.src.policy.repository.PolicyRepositoryCustom;
import com.anhtran.securemdm.src.policy.repository.projection.PolicyView;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class PolicyRepositoryCustomImpl extends SqlSupport implements PolicyRepositoryCustom {
  public PolicyRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Mono<Policy> lock(String organizationId, String id) {
    return one(
        Policy.class,
        "SELECT * FROM policies WHERE organization_id=? AND id=? FOR UPDATE",
        organizationId,
        id);
  }

  @Override
  public Flux<PolicyView> findLatestVersions(String organizationId, int size, long offset) {
    return all(
        PolicyView.class,
        "SELECT p.*,v.id AS version_id,v.version,v.config_json FROM policies p JOIN"
            + " policy_versions v ON v.policy_id=p.id AND v.organization_id=p.organization_id"
            + " WHERE p.organization_id=? AND v.version=(SELECT MAX(v2.version) FROM"
            + " policy_versions v2 WHERE v2.policy_id=p.id) ORDER BY p.created_at DESC,p.id LIMIT"
            + " ? OFFSET ?",
        organizationId,
        size,
        offset);
  }
}
