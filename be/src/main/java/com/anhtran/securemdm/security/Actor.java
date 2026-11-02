package com.anhtran.securemdm.security;

import com.anhtran.securemdm.exception.CommonException;

/**
 * Authenticated principal: an admin-console user (kind USER), a device agent (kind DEVICE) or the
 * scheduler (kind SYSTEM). Every query is scoped by {@code organizationId}.
 */
public record Actor(String organizationId, String id, String role, String kind, String sessionId) {
  public void requireAdmin() {
    if (!Role.ORG_ADMIN.name().equals(role)) {
      throw CommonException.forbidden("Administrator role required");
    }
  }

  public void requireOperator() {
    if (!Role.ORG_ADMIN.name().equals(role) && !Role.OPERATOR.name().equals(role)) {
      throw CommonException.forbidden("Operator role required");
    }
  }
}
