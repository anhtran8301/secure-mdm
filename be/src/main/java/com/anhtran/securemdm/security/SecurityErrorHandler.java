package com.anhtran.securemdm.security;

import com.anhtran.securemdm.exception.ErrorCode;
import com.anhtran.securemdm.exception.ErrorMessage;
import com.anhtran.securemdm.utils.JsonUtils;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Writes 401/403 in the same {code,message,status} shape as GlobalExceptionHandler. */
@Component
public class SecurityErrorHandler
    implements ServerAuthenticationEntryPoint, ServerAccessDeniedHandler {

  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
    return write(exchange, ErrorCode.UNAUTHORIZED);
  }

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException ex) {
    return write(exchange, ErrorCode.FORBIDDEN);
  }

  public Mono<Void> write(ServerWebExchange exchange, ErrorCode error) {
    var response = exchange.getResponse();
    response.setStatusCode(error.getStatus());
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    byte[] body = JsonUtils.write(ErrorMessage.of(error)).getBytes(StandardCharsets.UTF_8);
    return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
  }
}
