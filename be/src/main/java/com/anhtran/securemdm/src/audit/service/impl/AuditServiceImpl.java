package com.anhtran.securemdm.src.audit.service.impl;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.exception.CommonException;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.audit.dto.response.AuditLogResponse;
import com.anhtran.securemdm.src.audit.entity.AuditLog;
import com.anhtran.securemdm.src.audit.mapper.AuditLogMapper;
import com.anhtran.securemdm.src.audit.repository.AuditLogRepository;
import com.anhtran.securemdm.src.audit.service.AuditService;
import com.anhtran.securemdm.utils.AppConstants;
import com.anhtran.securemdm.utils.IdUtils;
import com.anhtran.securemdm.utils.PageUtils;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
  private final AuditLogRepository auditLogRepository;
  private final AuditLogMapper auditLogMapper;
  private final Clock clock;

  @Override
  public Mono<Void> record(Actor actor, String action, String resourceType, String resourceId) {
    return Mono.deferContextual(
        ctx ->
            auditLogRepository
                .save(
                    AuditLog.builder()
                        .id(IdUtils.newId())
                        .organizationId(actor.organizationId())
                        .actorType(actor.kind())
                        .actorId(actor.id())
                        .action(action)
                        .resourceType(resourceType)
                        .resourceId(resourceId)
                        .correlationId(
                            ctx.getOrDefault(AppConstants.CORRELATION_ID, IdUtils.newId()))
                        .createdAt(clock.millis())
                        .newEntity(true)
                        .build())
                .then());
  }

  @Override
  public Mono<PageResponse<AuditLogResponse>> list(Actor actor, String action, int page, int size) {
    return Mono.defer(
        () -> {
          long offset = PageUtils.offset(page, size);
          if (!action.matches("[A-Z_]{0,80}")) {
            throw CommonException.badRequest("Invalid action filter");
          }
          String org = actor.organizationId();
          return Mono.zip(
                  auditLogRepository
                      .findPage(org, action, size, offset)
                      .map(auditLogMapper::toResponse)
                      .collectList(),
                  auditLogRepository.countByAction(org, action))
              .map(p -> PageResponse.of(p.getT1(), p.getT2(), page, size));
        });
  }
}
