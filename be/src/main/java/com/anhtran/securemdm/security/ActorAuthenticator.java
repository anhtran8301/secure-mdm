package com.anhtran.securemdm.security;

import reactor.core.publisher.Mono;

/** Resolves a bearer credential (admin JWT or device credential) to the calling {@link Actor}. */
public interface ActorAuthenticator {
  /** Emits the actor, or errors with 401 when the credential is invalid, revoked or expired. */
  Mono<Actor> authenticate(String bearerToken);
}
