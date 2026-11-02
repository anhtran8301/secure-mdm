package com.anhtran.securemdm.src.identity.repository;

import com.anhtran.securemdm.src.identity.entity.RefreshToken;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface RefreshTokenRepository
    extends ReactiveCrudRepository<RefreshToken, String>, RefreshTokenRepositoryCustom {}
