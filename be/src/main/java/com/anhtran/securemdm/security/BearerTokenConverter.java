package com.anhtran.securemdm.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class BearerTokenConverter implements ServerAuthenticationConverter {
  private static final String PREFIX = "Bearer ";

  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (header == null || !header.startsWith(PREFIX)) {
      return Mono.empty();
    }
    return Mono.just(
        new UsernamePasswordAuthenticationToken("bearer", header.substring(PREFIX.length())));
  }
}
