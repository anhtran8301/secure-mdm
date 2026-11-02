package com.anhtran.securemdm.src.identity.dto.model;

import com.anhtran.securemdm.src.identity.dto.response.UserProfileResponse;

/** Issued session tokens; the refresh token only ever leaves the server as an HttpOnly cookie. */
public record AuthTokens(
    String accessToken, String refreshToken, long expiresAt, UserProfileResponse user) {}
