package com.anhtran.securemdm.src.identity.repository;

import com.anhtran.securemdm.src.identity.entity.User;
import reactor.core.publisher.Mono;

public interface UserRepositoryCustom {
  Mono<User> findActiveByOrganizationSlugAndEmail(String slug, String email);

  Mono<User> findActive(String organizationId, String id);

  /** Locks the user row; serializes the user's command idempotency namespace. */
  Mono<User> lock(String organizationId, String id);

  /** Active user owning an active, unexpired session. */
  Mono<User> findBySession(String sessionId, String userId, String organizationId, long now);

  Mono<Void> upsertPassword(User user);
}
