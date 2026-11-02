package com.anhtran.securemdm.src.device.repository;

import com.anhtran.securemdm.src.device.entity.EnrollmentToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface EnrollmentTokenRepository
    extends ReactiveCrudRepository<EnrollmentToken, String>, EnrollmentTokenRepositoryCustom {}
