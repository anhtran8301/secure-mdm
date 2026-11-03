package com.anhtran.securemdm.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Resolves an admin bearer JWT or a Device Proof to an {@link Actor}. */
@Component
@RequiredArgsConstructor
public class BearerAuthenticationManager implements ReactiveAuthenticationManager {
  private final ActorAuthenticator actorAuthenticator;

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    Object credentials = authentication.getCredentials();
    return (credentials instanceof DeviceProofRequest proof
            ? actorAuthenticator.authenticateDevice(proof)
            : actorAuthenticator.authenticate(credentials.toString()))
        .<Authentication>map(
            actor ->
                new UsernamePasswordAuthenticationToken(
                    actor, null, List.of(new SimpleGrantedAuthority("ROLE_" + actor.role()))))
        .onErrorMap(ex -> new BadCredentialsException("Invalid credential"));
  }
}
