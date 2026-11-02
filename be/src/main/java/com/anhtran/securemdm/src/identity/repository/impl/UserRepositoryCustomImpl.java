package com.anhtran.securemdm.src.identity.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.identity.entity.User;
import com.anhtran.securemdm.src.identity.repository.UserRepositoryCustom;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class UserRepositoryCustomImpl extends SqlSupport implements UserRepositoryCustom {
  public UserRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  private static final String COLUMNS = "u.id,u.organization_id,u.email,u.password_hash,u.role";

  @Override
  public Mono<User> findActiveByOrganizationSlugAndEmail(String slug, String email) {
    return one(
        User.class,
        "SELECT "
            + COLUMNS
            + " FROM users u JOIN organizations o ON o.id=u.organization_id"
            + " WHERE o.slug=? AND u.email=? AND u.active=TRUE",
        slug,
        email);
  }

  @Override
  public Mono<User> findActive(String organizationId, String id) {
    return one(
        User.class,
        "SELECT "
            + COLUMNS
            + " FROM users u WHERE u.id=? AND u.organization_id=? AND u.active=TRUE",
        id,
        organizationId);
  }

  @Override
  public Mono<User> lock(String organizationId, String id) {
    return one(
        User.class,
        "SELECT " + COLUMNS + " FROM users u WHERE u.organization_id=? AND u.id=? FOR UPDATE",
        organizationId,
        id);
  }

  @Override
  public Mono<User> findBySession(
      String sessionId, String userId, String organizationId, long now) {
    return one(
        User.class,
        "SELECT "
            + COLUMNS
            + " FROM auth_sessions s JOIN users u ON u.id=s.user_id AND"
            + " u.organization_id=s.organization_id WHERE s.id=? AND u.id=? AND s.organization_id=?"
            + " AND s.revoked_at IS NULL AND s.expires_at>? AND u.active=TRUE",
        sessionId,
        userId,
        organizationId,
        now);
  }

  @Override
  public Mono<Void> upsertPassword(User u) {
    return execute(
            "INSERT INTO users (id,organization_id,email,password_hash,role) VALUES (?,?,?,?,?)"
                + " ON DUPLICATE KEY UPDATE password_hash=VALUES(password_hash)",
            u.getId(),
            u.getOrganizationId(),
            u.getEmail(),
            u.getPasswordHash(),
            u.getRole())
        .then();
  }
}
