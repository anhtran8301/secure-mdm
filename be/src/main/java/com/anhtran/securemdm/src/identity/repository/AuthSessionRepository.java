package com.anhtran.securemdm.src.identity.repository;

import com.anhtran.securemdm.src.identity.entity.AuthSession;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AuthSessionRepository
    extends ReactiveCrudRepository<AuthSession, String>, AuthSessionRepositoryCustom {}
