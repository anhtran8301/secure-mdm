package com.anhtran.securemdm.security;

import com.anhtran.securemdm.utils.AppConstants;
import com.anhtran.securemdm.utils.IdUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/** Tags each request with X-Request-Id and a Reactor-context correlation id used by audit logs. */
@Component
public class CorrelationIdFilter implements WebFilter {
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String id = IdUtils.newId();
    exchange.getResponse().getHeaders().set("X-Request-Id", id);
    return chain.filter(exchange).contextWrite(ctx -> ctx.put(AppConstants.CORRELATION_ID, id));
  }
}
