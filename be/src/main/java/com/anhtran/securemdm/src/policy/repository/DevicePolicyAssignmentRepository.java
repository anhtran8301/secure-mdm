package com.anhtran.securemdm.src.policy.repository;

import com.anhtran.securemdm.src.policy.entity.DevicePolicyAssignment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface DevicePolicyAssignmentRepository
    extends ReactiveCrudRepository<DevicePolicyAssignment, String>,
        DevicePolicyAssignmentRepositoryCustom {}
