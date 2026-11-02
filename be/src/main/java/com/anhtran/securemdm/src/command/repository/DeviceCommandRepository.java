package com.anhtran.securemdm.src.command.repository;

import com.anhtran.securemdm.src.command.entity.DeviceCommand;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface DeviceCommandRepository
    extends ReactiveCrudRepository<DeviceCommand, String>, DeviceCommandRepositoryCustom {
  Mono<DeviceCommand> findByOrganizationIdAndId(String organizationId, String id);

  Mono<DeviceCommand> findByOrganizationIdAndCreatedByAndIdempotencyKey(
      String organizationId, String createdBy, String idempotencyKey);

  Mono<Long> countByOrganizationIdAndDeviceId(String organizationId, String deviceId);
}
