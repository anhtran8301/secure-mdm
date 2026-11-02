package com.anhtran.securemdm.src.command.repository;

import com.anhtran.securemdm.src.command.entity.DeviceCommand;
import com.anhtran.securemdm.src.command.repository.projection.CommandStatusCountView;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeviceCommandRepositoryCustom {
  /** PENDING/IN_PROGRESS commands of a device, oldest first. */
  Flux<DeviceCommand> findOpenForDevice(String organizationId, String deviceId);

  Mono<Long> transition(String organizationId, String id, String status);

  Mono<Long> countInProgress(String organizationId, String deviceId);

  /** Oldest pending command in insertion (FIFO) order. */
  Mono<DeviceCommand> findNextPending(String organizationId, String deviceId);

  Mono<Long> markClaimed(String organizationId, String id, String attemptId, long leaseUntil);

  Mono<Long> complete(
      String organizationId, String id, String status, String resultJson, long completedAt);

  /** Undelivered commands become CANCELLED; delivered-but-unfinished ones become UNKNOWN. */
  Mono<Long> cancelForDevice(String organizationId, String deviceId);

  Flux<DeviceCommand> findPage(String organizationId, String deviceId, int size, long offset);

  /** Devices (organizationId, deviceId only) with expired commands or leases to reconcile. */
  Flux<DeviceCommand> findDueDevices(long now);

  Flux<CommandStatusCountView> countByStatus(String organizationId);
}
