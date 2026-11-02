package com.anhtran.securemdm.src.audit.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.audit.entity.AuditLog;
import com.anhtran.securemdm.src.audit.repository.AuditLogRepositoryCustom;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class AuditLogRepositoryCustomImpl extends SqlSupport implements AuditLogRepositoryCustom {
  public AuditLogRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Flux<AuditLog> findPage(String organizationId, String action, int size, long offset) {
    return all(
        AuditLog.class,
        "SELECT * FROM audit_logs WHERE organization_id=? AND (?='' OR action=?) ORDER BY"
            + " created_at DESC,id LIMIT ? OFFSET ?",
        organizationId,
        action,
        action,
        size,
        offset);
  }

  @Override
  public Mono<Long> countByAction(String organizationId, String action) {
    return count(
        "SELECT COUNT(*) FROM audit_logs WHERE organization_id=? AND (?='' OR action=?)",
        organizationId,
        action,
        action);
  }
}
