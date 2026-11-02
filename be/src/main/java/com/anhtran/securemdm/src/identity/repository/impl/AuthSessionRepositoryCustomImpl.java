package com.anhtran.securemdm.src.identity.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.identity.repository.AuthSessionRepositoryCustom;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class AuthSessionRepositoryCustomImpl extends SqlSupport
    implements AuthSessionRepositoryCustom {
  public AuthSessionRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Mono<Long> revoke(String id, long at) {
    return execute("UPDATE auth_sessions SET revoked_at=? WHERE id=?", at, id);
  }
}
