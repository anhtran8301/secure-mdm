package com.anhtran.securemdm.src.audit.service;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.audit.dto.response.AuditLogResponse;
import reactor.core.publisher.Mono;

public interface AuditService {
  /** Appends an audit entry inside the caller's transaction; a failure rolls the change back. */
  Mono<Void> record(Actor actor, String action, String resourceType, String resourceId);

  Mono<PageResponse<AuditLogResponse>> list(Actor actor, String action, int page, int size);
}
