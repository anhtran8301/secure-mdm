package com.anhtran.securemdm.src.identity.repository;

import com.anhtran.securemdm.src.identity.entity.Organization;
import reactor.core.publisher.Mono;

public interface OrganizationRepositoryCustom {
  Mono<Void> insertIfAbsent(Organization organization);
}
