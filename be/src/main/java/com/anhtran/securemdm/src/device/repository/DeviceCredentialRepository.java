package com.anhtran.securemdm.src.device.repository;

import com.anhtran.securemdm.src.device.entity.DeviceCredential;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface DeviceCredentialRepository
    extends ReactiveCrudRepository<DeviceCredential, String>, DeviceCredentialRepositoryCustom {}
