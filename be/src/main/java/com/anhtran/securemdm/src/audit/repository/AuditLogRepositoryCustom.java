package com.anhtran.securemdm.src.audit.repository;

import com.anhtran.securemdm.src.audit.entity.AuditLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AuditLogRepositoryCustom {
  /** An empty {@code action} matches every action. */
  Flux<AuditLog> findPage(String organizationId, String action, int size, long offset);

  Mono<Long> countByAction(String organizationId, String action);
}
