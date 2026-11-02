package com.anhtran.securemdm.src.identity.controller;

import com.anhtran.securemdm.config.MdmProperties;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.identity.dto.model.AuthTokens;
import com.anhtran.securemdm.src.identity.dto.request.LoginRequest;
import com.anhtran.securemdm.src.identity.dto.response.CsrfTokenResponse;
import com.anhtran.securemdm.src.identity.dto.response.TokenResponse;
import com.anhtran.securemdm.src.identity.dto.response.UserProfileResponse;
import com.anhtran.securemdm.src.identity.service.AuthService;
import com.anhtran.securemdm.utils.AppConstants;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Refresh token travels only in an HttpOnly, SameSite=Strict cookie scoped to /api/v1/auth. */
@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_PREFIX + "/auth")
public class AuthController {
  private static final String COOKIE_PATH = AppConstants.API_PREFIX + "/auth";

  private final AuthService authService;
  private final MdmProperties properties;
  private final Clock clock;

  @GetMapping("/csrf")
  public Mono<CsrfTokenResponse> csrf(ServerWebExchange exchange) {
    Mono<CsrfToken> token = exchange.getAttribute(CsrfToken.class.getName());
    return token.map(t -> new CsrfTokenResponse(t.getToken(), t.getHeaderName()));
  }

  @PostMapping("/login")
  public Mono<TokenResponse> login(
      @Valid @RequestBody LoginRequest request, ServerWebExchange exchange) {
    return authService.login(request).map(tokens -> respond(tokens, exchange));
  }

  @PostMapping("/refresh")
  public Mono<TokenResponse> refresh(ServerWebExchange exchange) {
    return authService.refresh(refreshCookie(exchange)).map(tokens -> respond(tokens, exchange));
  }

  @PostMapping("/logout")
  public Mono<Void> logout(ServerWebExchange exchange) {
    return authService
        .logout(refreshCookie(exchange))
        .doOnSuccess(v -> setRefreshCookie(exchange, "", 0));
  }

  @GetMapping("/me")
  public Mono<UserProfileResponse> me(@AuthenticationPrincipal Actor actor) {
    return authService.profile(actor);
  }

  private static String refreshCookie(ServerWebExchange exchange) {
    var cookie = exchange.getRequest().getCookies().getFirst(AppConstants.REFRESH_COOKIE);
    return cookie == null ? null : cookie.getValue();
  }

  private void setRefreshCookie(ServerWebExchange exchange, String token, long seconds) {
    exchange
        .getResponse()
        .addCookie(
            ResponseCookie.from(AppConstants.REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(properties.secureCookies())
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofSeconds(seconds))
                .build());
  }

  private TokenResponse respond(AuthTokens tokens, ServerWebExchange exchange) {
    setRefreshCookie(
        exchange, tokens.refreshToken(), Math.max(0, (tokens.expiresAt() - clock.millis()) / 1000));
    return new TokenResponse(tokens.accessToken(), tokens.user());
  }
}
