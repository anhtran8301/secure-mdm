package com.anhtran.securemdm.src.identity.repository.projection;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Refresh token joined with its session (and user role) under a row lock. */
@Getter
@Setter
@NoArgsConstructor
public class RefreshSessionView {
  private String id;
  private String organizationId;
  private String userId;
  private Long expiresAt;
  private Long revokedAt;
  private Long usedAt;
  private String role;
}
