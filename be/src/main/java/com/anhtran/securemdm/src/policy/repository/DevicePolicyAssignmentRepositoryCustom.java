package com.anhtran.securemdm.src.policy.repository;

import com.anhtran.securemdm.src.policy.entity.DevicePolicyAssignment;
import com.anhtran.securemdm.src.policy.repository.projection.AssignedVersionView;
import reactor.core.publisher.Mono;

public interface DevicePolicyAssignmentRepositoryCustom {
  Mono<Void> upsert(DevicePolicyAssignment assignment);

  Mono<Long> deleteForDevice(String organizationId, String deviceId);

  Mono<AssignedVersionView> findAssignedVersion(String organizationId, String deviceId);
}
