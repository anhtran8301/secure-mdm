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
  private static final String DEVICE_PREFIX = "DeviceProof ";

  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    var request = exchange.getRequest();
    String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (header != null && header.startsWith(DEVICE_PREFIX)) {
      String query = request.getURI().getRawQuery();
      String target = request.getURI().getRawPath() + (query == null ? "" : "?" + query);
      return Mono.just(
          new UsernamePasswordAuthenticationToken(
              "device",
              new DeviceProofRequest(
                  header.substring(DEVICE_PREFIX.length()), request.getMethod().name(), target)));
    }
    if (header == null || !header.startsWith(PREFIX)) {
      return Mono.empty();
    }
    return Mono.just(
        new UsernamePasswordAuthenticationToken("bearer", header.substring(PREFIX.length())));
  }
}
