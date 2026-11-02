package com.anhtran.securemdm.src.identity.service;

import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.security.ActorAuthenticator;
import com.anhtran.securemdm.src.identity.dto.model.AuthTokens;
import com.anhtran.securemdm.src.identity.dto.request.LoginRequest;
import com.anhtran.securemdm.src.identity.dto.response.UserProfileResponse;
import reactor.core.publisher.Mono;

public interface AuthService extends ActorAuthenticator {
  Mono<AuthTokens> login(LoginRequest request);

  /** Rotates the refresh token; reusing a spent token revokes the whole session. */
  Mono<AuthTokens> refresh(String refreshToken);

  Mono<Void> logout(String refreshToken);

  Mono<UserProfileResponse> profile(Actor actor);
}
