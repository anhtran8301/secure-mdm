package com.anhtran.securemdm.src.identity.repository;

import reactor.core.publisher.Mono;

public interface AuthSessionRepositoryCustom {
  Mono<Long> revoke(String id, long at);
}
