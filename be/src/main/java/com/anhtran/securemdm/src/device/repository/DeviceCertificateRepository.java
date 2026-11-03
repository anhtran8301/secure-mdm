package com.anhtran.securemdm.src.device.repository;

import com.anhtran.securemdm.src.device.entity.DeviceCertificate;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface DeviceCertificateRepository
    extends ReactiveCrudRepository<DeviceCertificate, String>, DeviceCertificateRepositoryCustom {}
