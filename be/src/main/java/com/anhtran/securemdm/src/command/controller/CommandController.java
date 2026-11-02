package com.anhtran.securemdm.src.command.controller;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.command.dto.request.CreateCommandRequest;
import com.anhtran.securemdm.src.command.dto.response.CommandResponse;
import com.anhtran.securemdm.src.command.service.CommandService;
import com.anhtran.securemdm.utils.AppConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_PREFIX + "/devices/{deviceId}/commands")
public class CommandController {
  private final CommandService commandService;

  @PostMapping
  public Mono<CommandResponse> create(
      @AuthenticationPrincipal Actor actor,
      @PathVariable String deviceId,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody CreateCommandRequest request) {
    return commandService.create(actor, deviceId, request, idempotencyKey);
  }

  @GetMapping
  public Mono<PageResponse<CommandResponse>> list(
      @AuthenticationPrincipal Actor actor,
      @PathVariable String deviceId,
      @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
      @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size) {
    return commandService.list(actor, deviceId, page, size);
  }
}
