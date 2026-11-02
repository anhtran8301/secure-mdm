package com.anhtran.securemdm.src.identity.repository;

import com.anhtran.securemdm.src.identity.entity.Organization;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OrganizationRepository
    extends ReactiveCrudRepository<Organization, String>, OrganizationRepositoryCustom {}
