package com.anhtran.securemdm.src.policy.repository;

import com.anhtran.securemdm.src.policy.entity.DevicePolicyReport;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface DevicePolicyReportRepository
    extends ReactiveCrudRepository<DevicePolicyReport, String>, DevicePolicyReportRepositoryCustom {
  Mono<DevicePolicyReport> findByOrganizationIdAndDeviceId(String organizationId, String deviceId);
}
