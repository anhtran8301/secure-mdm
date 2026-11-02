package com.anhtran.securemdm.src.policy.repository;

import com.anhtran.securemdm.src.policy.entity.DevicePolicyReport;
import reactor.core.publisher.Mono;

public interface DevicePolicyReportRepositoryCustom {
  Mono<Void> upsert(DevicePolicyReport report);
}
