package com.anhtran.securemdm.src.command.controller;

import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.command.dto.request.CommandResultRequest;
import com.anhtran.securemdm.src.command.dto.response.CommandClaimResponse;
import com.anhtran.securemdm.src.command.dto.response.CommandKeysResponse;
import com.anhtran.securemdm.src.command.dto.response.CommandResponse;
import com.anhtran.securemdm.src.command.service.CommandService;
import com.anhtran.securemdm.utils.AppConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Endpoints called by an enrolled device; the principal is the device itself (role DEVICE). */
@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstants.API_PREFIX + "/device-agent")
public class CommandAgentController {
  private final CommandService commandService;

  /** Signed Trust Bundles so the device can follow command-key rotation (ADR 0006). */
  @GetMapping("/command-keys")
  public Mono<CommandKeysResponse> commandKeys() {
    return commandService.commandKeys();
  }

  /** 200 with a leased command, or 204 when nothing is due. */
  @PostMapping("/commands/claim")
  public Mono<ResponseEntity<CommandClaimResponse>> claim(@AuthenticationPrincipal Actor device) {
    return commandService
        .claim(device)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.noContent().build());
  }

  @PostMapping("/commands/{id}/result")
  public Mono<CommandResponse> result(
      @AuthenticationPrincipal Actor device,
      @PathVariable String id,
      @Valid @RequestBody CommandResultRequest request) {
    return commandService.result(device, id, request);
  }
}
