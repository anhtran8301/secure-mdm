package com.anhtran.securemdm.src.identity.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.identity.entity.Organization;
import com.anhtran.securemdm.src.identity.repository.OrganizationRepositoryCustom;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class OrganizationRepositoryCustomImpl extends SqlSupport
    implements OrganizationRepositoryCustom {
  public OrganizationRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Mono<Void> insertIfAbsent(Organization o) {
    return execute(
            "INSERT IGNORE INTO organizations (id,slug,name) VALUES (?,?,?)",
            o.getId(),
            o.getSlug(),
            o.getName())
        .then();
  }
}
