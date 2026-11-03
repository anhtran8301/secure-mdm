package com.anhtran.securemdm.src.command.service.impl;

import com.anhtran.securemdm.common.dto.PageResponse;
import com.anhtran.securemdm.config.MdmProperties;
import com.anhtran.securemdm.exception.CommonException;
import com.anhtran.securemdm.security.Actor;
import com.anhtran.securemdm.security.ActorKind;
import com.anhtran.securemdm.security.CommandSigner;
import com.anhtran.securemdm.security.Role;
import com.anhtran.securemdm.security.SecureTokens;
import com.anhtran.securemdm.src.audit.service.AuditService;
import com.anhtran.securemdm.src.command.dto.request.CommandResultRequest;
import com.anhtran.securemdm.src.command.dto.request.CreateCommandRequest;
import com.anhtran.securemdm.src.command.dto.response.CommandClaimResponse;
import com.anhtran.securemdm.src.command.dto.response.CommandKeysResponse;
import com.anhtran.securemdm.src.command.dto.response.CommandResponse;
import com.anhtran.securemdm.src.command.entity.CommandAttempt;
import com.anhtran.securemdm.src.command.entity.DeviceCommand;
import com.anhtran.securemdm.src.command.entity.enums.CommandStatus;
import com.anhtran.securemdm.src.command.mapper.CommandMapper;
import com.anhtran.securemdm.src.command.repository.CommandAttemptRepository;
import com.anhtran.securemdm.src.command.repository.DeviceCommandRepository;
import com.anhtran.securemdm.src.command.service.CommandService;
import com.anhtran.securemdm.src.command.service.rule.CommandStateMachine;
import com.anhtran.securemdm.src.device.entity.enums.DeviceStatus;
import com.anhtran.securemdm.src.device.repository.DeviceRepository;
import com.anhtran.securemdm.src.identity.repository.UserRepository;
import com.anhtran.securemdm.utils.IdUtils;
import com.anhtran.securemdm.utils.JsonUtils;
import com.anhtran.securemdm.utils.PageUtils;
import java.time.Clock;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CommandServiceImpl implements CommandService {
  private static final String COMMAND = "command";
  private static final String IN_PROGRESS = CommandStatus.IN_PROGRESS.name();

  private final DeviceCommandRepository commandRepository;
  private final CommandAttemptRepository attemptRepository;
  private final DeviceRepository deviceRepository;
  private final UserRepository userRepository;
  private final AuditService auditService;
  private final CommandSigner commandSigner;
  private final CommandMapper commandMapper;
  private final MdmProperties properties;
  private final TransactionalOperator transactions;
  private final Clock clock;

  @Override
  public Mono<CommandResponse> create(
      Actor actor, String deviceId, CreateCommandRequest request, String idempotencyKey) {
    return Mono.defer(
        () -> {
          actor.requireOperator();
          CommandStateMachine.validate(request.type(), request.payload());
          if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9_-]{1,100}")) {
            throw CommonException.badRequest("Valid Idempotency-Key required");
          }
          String org = actor.organizationId();
          String hash =
              SecureTokens.hash(
                  JsonUtils.write(
                      Map.of(
                          "device", deviceId,
                          "type", request.type(),
                          "payload", request.payload())));
          // Serialize the actor's idempotency namespace, including requests for other
          // devices.
          return userRepository
              .lock(org, actor.id())
              .then(deviceRepository.lockExisting(org, deviceId))
              .flatMap(
                  device -> {
                    if (!DeviceStatus.ACTIVE.name().equals(device.getStatus())) {
                      return Mono.error(CommonException.conflict("Device revoked"));
                    }
                    return commandRepository
                        .findByOrganizationIdAndCreatedByAndIdempotencyKey(
                            org, actor.id(), idempotencyKey)
                        .flatMap(
                            existing ->
                                hash.equals(existing.getRequestHash())
                                    ? Mono.just(existing)
                                    : Mono.error(
                                        CommonException.conflict(
                                            "Idempotency key already used for different request")))
                        .switchIfEmpty(
                            Mono.defer(
                                () -> insert(actor, deviceId, request, idempotencyKey, hash)));
                  })
              .map(commandMapper::toResponse)
              .as(transactions::transactional);
        });
  }

  private Mono<DeviceCommand> insert(
      Actor actor, String deviceId, CreateCommandRequest request, String key, String hash) {
    String id = IdUtils.newId();
    long now = clock.millis();
    // JWT exp has second precision; truncate so the stored expiry matches the
    // signed one.
    long expiresAt = ((now + properties.commandTtlMs()) / 1000) * 1000;
    String signed =
        commandSigner.sign(
            id, actor.organizationId(), deviceId, request.type(), request.payload(), expiresAt);
    return commandRepository
        .save(
            DeviceCommand.builder()
                .id(id)
                .organizationId(actor.organizationId())
                .deviceId(deviceId)
                .createdBy(actor.id())
                .type(request.type())
                .payloadJson(JsonUtils.write(request.payload()))
                .requestHash(hash)
                .idempotencyKey(key)
                .status(CommandStatus.PENDING.name())
                .signedCommand(signed)
                .attempts(0)
                .createdAt(now)
                .expiresAt(expiresAt)
                .newEntity(true)
                .build())
        .flatMap(
            saved -> auditService.record(actor, "COMMAND_CREATED", COMMAND, id).thenReturn(saved));
  }

  @Override
  public Mono<Void> reconcile(String organizationId, String deviceId) {
    Actor system =
        new Actor(organizationId, deviceId, Role.DEVICE.name(), ActorKind.SYSTEM.name(), null);
    return commandRepository
        .findOpenForDevice(organizationId, deviceId)
        .concatMap(
            command -> {
              String next =
                  CommandStateMachine.transition(
                      command.getStatus(),
                      command.getAttempts(),
                      clock.millis(),
                      command.getExpiresAt(),
                      command.getLeaseUntil(),
                      properties.maxAttempts());
              if (next.equals(command.getStatus())) {
                return Mono.empty();
              }
              return commandRepository
                  .transition(organizationId, command.getId(), next)
                  .then(auditService.record(system, "COMMAND_" + next, COMMAND, command.getId()));
            })
        .then();
  }

  @Override
  public Mono<CommandClaimResponse> claim(Actor device) {
    String org = device.organizationId();
    return deviceRepository
        .lockExisting(org, device.id())
        .flatMap(
            d -> {
              if (!DeviceStatus.ACTIVE.name().equals(d.getStatus())) {
                return Mono.error(CommonException.unauthorized());
              }
              return reconcile(org, device.id())
                  .then(commandRepository.countInProgress(org, device.id()))
                  .flatMap(
                      inProgress ->
                          inProgress > 0
                              ? Mono.<DeviceCommand>empty()
                              : commandRepository.findNextPending(org, device.id()))
                  .flatMap(command -> lease(device, command));
            })
        .as(transactions::transactional);
  }

  private Mono<CommandClaimResponse> lease(Actor device, DeviceCommand command) {
    String org = device.organizationId();
    String attemptId = IdUtils.newId();
    long now = clock.millis();
    long leaseUntil = Math.min(now + properties.leaseMs(), command.getExpiresAt());
    return attemptRepository
        .save(
            CommandAttempt.builder()
                .id(attemptId)
                .organizationId(org)
                .commandId(command.getId())
                .claimedAt(now)
                .leaseUntil(leaseUntil)
                .newEntity(true)
                .build())
        .then(commandRepository.markClaimed(org, command.getId(), attemptId, leaseUntil))
        .then(auditService.record(device, "COMMAND_CLAIMED", COMMAND, command.getId()))
        .thenReturn(
            new CommandClaimResponse(
                command.getId(), attemptId, leaseUntil, command.getSignedCommand()));
  }

  @Override
  public Mono<CommandResponse> result(
      Actor device, String commandId, CommandResultRequest request) {
    String org = device.organizationId();
    return deviceRepository
        .lockExisting(org, device.id())
        .flatMap(
            d -> {
              if (!DeviceStatus.ACTIVE.name().equals(d.getStatus())) {
                return Mono.error(CommonException.unauthorized());
              }
              return findExisting(org, commandId)
                  .flatMap(command -> complete(device, command, request));
            })
        .map(commandMapper::toResponse)
        .as(transactions::transactional);
  }

  private Mono<DeviceCommand> complete(
      Actor device, DeviceCommand command, CommandResultRequest request) {
    if (!device.id().equals(command.getDeviceId())) {
      return Mono.error(CommonException.notFound());
    }
    String status = request.status();
    String attemptId = request.attemptId();
    CommandStateMachine.validateResult(command.getType(), status, request.result());
    String resultJson = JsonUtils.write(request.result());
    // An identical result for the accepted attempt is an idempotent retry.
    if (attemptId.equals(command.getCurrentAttemptId())
        && status.equals(command.getStatus())
        && resultJson.equals(canonical(command.getResultJson()))) {
      return Mono.just(command);
    }
    long now = clock.millis();
    if (!IN_PROGRESS.equals(command.getStatus())
        || !attemptId.equals(command.getCurrentAttemptId())
        || now >= command.getLeaseUntil()
        || now >= command.getExpiresAt()) {
      return Mono.error(CommonException.conflict("Attempt is no longer current"));
    }
    String org = device.organizationId();
    return commandRepository
        .complete(org, command.getId(), status, resultJson, now)
        .then(auditService.record(device, "COMMAND_" + status, COMMAND, command.getId()))
        .then(findExisting(org, command.getId()));
  }

  private static String canonical(String json) {
    return JsonUtils.write(JsonUtils.read(json, Object.class));
  }

  private Mono<DeviceCommand> findExisting(String organizationId, String commandId) {
    return commandRepository
        .findByOrganizationIdAndId(organizationId, commandId)
        .switchIfEmpty(Mono.error(CommonException.notFound()));
  }

  @Override
  public Mono<PageResponse<CommandResponse>> list(
      Actor actor, String deviceId, int page, int size) {
    return Mono.defer(
        () -> {
          long offset = PageUtils.offset(page, size);
          String org = actor.organizationId();
          return deviceRepository
              .findExisting(org, deviceId)
              .then(
                  Mono.zip(
                      commandRepository
                          .findPage(org, deviceId, size, offset)
                          .map(commandMapper::toResponse)
                          .collectList(),
                      commandRepository.countByOrganizationIdAndDeviceId(org, deviceId)))
              .map(p -> PageResponse.of(p.getT1(), p.getT2(), page, size));
        });
  }

  @Override
  public Mono<Void> sweep() {
    return commandRepository
        .findDueDevices(clock.millis())
        .concatMap(
            due ->
                deviceRepository
                    .lockExisting(due.getOrganizationId(), due.getDeviceId())
                    .then(reconcile(due.getOrganizationId(), due.getDeviceId()))
                    .as(transactions::transactional))
        .then();
  }

  @Override
  public Mono<CommandKeysResponse> commandKeys() {
    return Mono.fromSupplier(() -> new CommandKeysResponse(commandSigner.trustBundles()));
  }
}
