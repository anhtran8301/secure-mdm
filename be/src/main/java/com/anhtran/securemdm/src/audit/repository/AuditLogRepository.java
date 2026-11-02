package com.anhtran.securemdm.src.audit.repository;

import com.anhtran.securemdm.src.audit.entity.AuditLog;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AuditLogRepository
    extends ReactiveCrudRepository<AuditLog, String>, AuditLogRepositoryCustom {}
