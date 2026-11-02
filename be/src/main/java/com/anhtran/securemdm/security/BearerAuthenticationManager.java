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

/** Resolves a bearer credential (admin JWT or opaque device credential) to an {@link Actor}. */
@Component
@RequiredArgsConstructor
public class BearerAuthenticationManager implements ReactiveAuthenticationManager {
  private final ActorAuthenticator actorAuthenticator;

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    return actorAuthenticator
        .authenticate(authentication.getCredentials().toString())
        .<Authentication>map(
            actor ->
                new UsernamePasswordAuthenticationToken(
                    actor, null, List.of(new SimpleGrantedAuthority("ROLE_" + actor.role()))))
        .onErrorMap(ex -> new BadCredentialsException("Invalid credential"));
  }
}
