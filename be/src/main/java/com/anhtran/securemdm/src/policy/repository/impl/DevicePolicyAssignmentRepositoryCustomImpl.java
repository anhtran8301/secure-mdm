package com.anhtran.securemdm.src.policy.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.policy.entity.DevicePolicyAssignment;
import com.anhtran.securemdm.src.policy.repository.DevicePolicyAssignmentRepositoryCustom;
import com.anhtran.securemdm.src.policy.repository.projection.AssignedVersionView;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class DevicePolicyAssignmentRepositoryCustomImpl extends SqlSupport
    implements DevicePolicyAssignmentRepositoryCustom {
  public DevicePolicyAssignmentRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Mono<Void> upsert(DevicePolicyAssignment a) {
    return execute(
            "INSERT INTO device_policy_assignments"
                + " (device_id,organization_id,version_id,revision,assigned_at) VALUES (?,?,?,?,?)"
                + " ON DUPLICATE KEY UPDATE version_id=VALUES(version_id),"
                + "revision=VALUES(revision),assigned_at=VALUES(assigned_at)",
            a.getDeviceId(),
            a.getOrganizationId(),
            a.getVersionId(),
            a.getRevision(),
            a.getAssignedAt())
        .then();
  }

  @Override
  public Mono<Long> deleteForDevice(String organizationId, String deviceId) {
    return execute(
        "DELETE FROM device_policy_assignments WHERE organization_id=? AND device_id=?",
        organizationId,
        deviceId);
  }

  @Override
  public Mono<AssignedVersionView> findAssignedVersion(String organizationId, String deviceId) {
    return one(
        AssignedVersionView.class,
        "SELECT v.*,a.revision FROM device_policy_assignments a JOIN policy_versions v ON"
            + " v.id=a.version_id AND v.organization_id=a.organization_id WHERE"
            + " a.organization_id=? AND a.device_id=?",
        organizationId,
        deviceId);
  }
}
