package com.anhtran.securemdm.src.identity.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.identity.repository.RefreshTokenRepositoryCustom;
import com.anhtran.securemdm.src.identity.repository.projection.RefreshSessionView;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class RefreshTokenRepositoryCustomImpl extends SqlSupport
    implements RefreshTokenRepositoryCustom {
  public RefreshTokenRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Mono<RefreshSessionView> lockForRefresh(String tokenHash) {
    return one(
        RefreshSessionView.class,
        "SELECT s.*, t.used_at FROM refresh_tokens t JOIN auth_sessions s ON s.id=t.session_id"
            + " WHERE t.token_hash=? FOR UPDATE",
        tokenHash);
  }

  @Override
  public Mono<RefreshSessionView> lockForLogout(String tokenHash) {
    return one(
        RefreshSessionView.class,
        "SELECT s.*, t.used_at, u.role FROM refresh_tokens t JOIN auth_sessions s ON"
            + " s.id=t.session_id JOIN users u ON u.id=s.user_id WHERE t.token_hash=? FOR UPDATE",
        tokenHash);
  }

  @Override
  public Mono<Long> markUsed(String tokenHash, long at) {
    return execute("UPDATE refresh_tokens SET used_at=? WHERE token_hash=?", at, tokenHash);
  }
}
