package com.anhtran.securemdm.src.device.repository;

import com.anhtran.securemdm.src.device.entity.Device;
import com.anhtran.securemdm.src.device.repository.projection.DeviceStatsView;
import com.anhtran.securemdm.src.device.repository.projection.DeviceView;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DeviceRepositoryCustom {
  /** Locks the device row; every device state change is serialized through this lock. */
  Mono<Device> lock(String organizationId, String id);

  Mono<Long> updateInventory(String organizationId, String id, String inventoryJson, long seenAt);

  Mono<Long> markRevoked(String organizationId, String id);

  Mono<Long> incrementAssignmentRevision(String organizationId, String id);

  /** Devices last seen before {@code cutoff} are offline and their compliance is unknown. */
  Mono<DeviceView> findView(String organizationId, String id, long cutoff);

  Flux<DeviceView> findViews(
      String organizationId, String namePattern, long cutoff, int size, long offset);

  Mono<Long> countByNamePattern(String organizationId, String namePattern);

  Mono<DeviceStatsView> stats(String organizationId, long onlineSince);
}
