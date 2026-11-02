package com.anhtran.securemdm.src.policy.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.policy.entity.DevicePolicyReport;
import com.anhtran.securemdm.src.policy.repository.DevicePolicyReportRepositoryCustom;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class DevicePolicyReportRepositoryCustomImpl extends SqlSupport
    implements DevicePolicyReportRepositoryCustom {
  public DevicePolicyReportRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Mono<Void> upsert(DevicePolicyReport r) {
    return execute(
            "INSERT INTO device_policy_reports"
                + " (device_id,organization_id,revision,version_id,compliance,"
                + "reasons_json,reported_at)"
                + " VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE revision=VALUES(revision),"
                + "version_id=VALUES(version_id),compliance=VALUES(compliance),"
                + "reasons_json=VALUES(reasons_json),reported_at=VALUES(reported_at)",
            r.getDeviceId(),
            r.getOrganizationId(),
            r.getRevision(),
            r.getVersionId(),
            r.getCompliance(),
            r.getReasonsJson(),
            r.getReportedAt())
        .then();
  }
}
