package com.anhtran.securemdm.security;

import reactor.core.publisher.Mono;

/** Resolves a request credential (admin JWT or Device Proof) to the calling {@link Actor}. */
public interface ActorAuthenticator {
  /** Emits the actor, or errors with 401 when the credential is invalid, revoked or expired. */
  Mono<Actor> authenticate(String bearerToken);

  /** Emits the device actor, or errors with 401 when the proof fails any check (ADR 0005). */
  Mono<Actor> authenticateDevice(DeviceProofRequest request);
}
