package com.anhtran.securemdm.src.audit.controller;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.audit.dto.response.AuditLogResponse;
import com.anhtran.securemdm.src.audit.service.AuditService;
import com.anhtran.securemdm.utils.AppConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_PREFIX + "/audit-logs")
public class AuditController {
  private final AuditService auditService;

  @GetMapping
  public Mono<PageResponse<AuditLogResponse>> list(
      @AuthenticationPrincipal Actor actor,
      @RequestParam(defaultValue = "") String action,
      @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
      @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
    return auditService.list(actor, action, page, size);
  }
}
