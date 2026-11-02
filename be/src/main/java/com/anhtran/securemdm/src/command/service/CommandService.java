package com.anhtran.securemdm.src.command.service;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.src.command.dto.request.CommandResultRequest;
import com.anhtran.securemdm.src.command.dto.request.CreateCommandRequest;
import com.anhtran.securemdm.src.command.dto.response.CommandClaimResponse;
import com.anhtran.securemdm.src.command.dto.response.CommandResponse;
import reactor.core.publisher.Mono;

public interface CommandService {
  /** The Idempotency-Key binds the request contents; reuse with other contents is a 409. */
  Mono<CommandResponse> create(
      Actor actor, String deviceId, CreateCommandRequest request, String idempotencyKey);

  Mono<PageResponse<CommandResponse>> list(Actor actor, String deviceId, int page, int size);

  /** Leases the oldest pending command (FIFO, one active lease); empty when none is due. */
  Mono<CommandClaimResponse> claim(Actor device);

  Mono<CommandResponse> result(Actor device, String commandId, CommandResultRequest request);

  /** Applies expiry and lease timeouts for one device. Caller must hold the device lock. */
  Mono<Void> reconcile(String organizationId, String deviceId);

  Mono<Void> sweep();
}
