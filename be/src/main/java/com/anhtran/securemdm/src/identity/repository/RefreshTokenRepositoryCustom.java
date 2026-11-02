package com.anhtran.securemdm.src.identity.repository;

import com.anhtran.securemdm.src.identity.repository.projection.RefreshSessionView;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepositoryCustom {
  /** Locks the token and its session; {@code role} is not populated. */
  Mono<RefreshSessionView> lockForRefresh(String tokenHash);

  /** Locks the token, its session and user; {@code role} is populated. */
  Mono<RefreshSessionView> lockForLogout(String tokenHash);

  Mono<Long> markUsed(String tokenHash, long at);
}
