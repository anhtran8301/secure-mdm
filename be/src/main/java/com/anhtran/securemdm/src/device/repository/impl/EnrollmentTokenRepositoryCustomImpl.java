package com.anhtran.securemdm.src.device.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.device.entity.EnrollmentToken;
import com.anhtran.securemdm.src.device.repository.EnrollmentTokenRepositoryCustom;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class EnrollmentTokenRepositoryCustomImpl extends SqlSupport
    implements EnrollmentTokenRepositoryCustom {
  public EnrollmentTokenRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Mono<EnrollmentToken> lockByTokenHash(String tokenHash) {
    return one(
        EnrollmentToken.class,
        "SELECT * FROM enrollment_tokens WHERE token_hash=? FOR UPDATE",
        tokenHash);
  }

  @Override
  public Mono<Long> markUsed(String id, long at) {
    return execute("UPDATE enrollment_tokens SET used_at=? WHERE id=?", at, id);
  }

  @Override
  public Mono<Long> revoke(String organizationId, String id, long at) {
    return execute(
        "UPDATE enrollment_tokens SET revoked_at=? WHERE organization_id=? AND id=?",
        at,
        organizationId,
        id);
  }
}
