package com.anhtran.securemdm.src.device.repository;

import com.anhtran.securemdm.exception.CommonException;
import com.anhtran.securemdm.src.device.entity.Device;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface DeviceRepository
    extends ReactiveCrudRepository<Device, String>, DeviceRepositoryCustom {
  Mono<Device> findByOrganizationIdAndId(String organizationId, String id);

  /** Tenant-scoped lookup; a foreign or missing device is a 404. */
  default Mono<Device> findExisting(String organizationId, String id) {
    return findByOrganizationIdAndId(organizationId, id)
        .switchIfEmpty(Mono.error(CommonException.notFound()));
  }

  default Mono<Device> lockExisting(String organizationId, String id) {
    return lock(organizationId, id).switchIfEmpty(Mono.error(CommonException.notFound()));
  }
}
