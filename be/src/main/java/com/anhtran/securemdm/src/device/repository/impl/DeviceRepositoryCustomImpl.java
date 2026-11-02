package com.anhtran.securemdm.src.device.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.device.entity.Device;
import com.anhtran.securemdm.src.device.repository.DeviceRepositoryCustom;
import com.anhtran.securemdm.src.device.repository.projection.DeviceStatsView;
import com.anhtran.securemdm.src.device.repository.projection.DeviceView;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class DeviceRepositoryCustomImpl extends SqlSupport implements DeviceRepositoryCustom {
  public DeviceRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  private static final String VIEW =
      "SELECT d.*, CASE WHEN d.status='REVOKED' THEN 'REVOKED' WHEN d.last_seen_at IS NULL OR"
          + " d.last_seen_at<? THEN 'OFFLINE' ELSE 'ONLINE' END AS connectivity, CASE WHEN"
          + " d.status='REVOKED' OR d.last_seen_at IS NULL OR d.last_seen_at<? THEN 'UNKNOWN'"
          + " WHEN a.device_id IS NULL THEN 'UNKNOWN' WHEN r.revision IS NULL OR"
          + " r.revision<>a.revision THEN 'PENDING' WHEN r.reported_at<? THEN 'UNKNOWN' ELSE"
          + " r.compliance END AS compliance FROM devices d LEFT JOIN device_policy_assignments a"
          + " ON a.device_id=d.id LEFT JOIN device_policy_reports r ON r.device_id=d.id ";

  @Override
  public Mono<Device> lock(String organizationId, String id) {
    return one(
        Device.class,
        "SELECT * FROM devices WHERE organization_id=? AND id=? FOR UPDATE",
        organizationId,
        id);
  }

  @Override
  public Mono<Long> updateInventory(
      String organizationId, String id, String inventoryJson, long seenAt) {
    return execute(
        "UPDATE devices SET inventory_json=?,last_seen_at=? WHERE organization_id=? AND id=?",
        inventoryJson,
        seenAt,
        organizationId,
        id);
  }

  @Override
  public Mono<Long> markRevoked(String organizationId, String id) {
    return execute(
        "UPDATE devices SET status='REVOKED' WHERE organization_id=? AND id=?", organizationId, id);
  }

  @Override
  public Mono<Long> incrementAssignmentRevision(String organizationId, String id) {
    return execute(
        "UPDATE devices SET assignment_revision=assignment_revision+1 WHERE organization_id=? AND"
            + " id=?",
        organizationId,
        id);
  }

  @Override
  public Mono<DeviceView> findView(String organizationId, String id, long cutoff) {
    return one(
        DeviceView.class,
        VIEW + "WHERE d.organization_id=? AND d.id=?",
        cutoff,
        cutoff,
        cutoff,
        organizationId,
        id);
  }

  @Override
  public Flux<DeviceView> findViews(
      String organizationId, String namePattern, long cutoff, int size, long offset) {
    return all(
        DeviceView.class,
        VIEW
            + "WHERE d.organization_id=? AND d.name LIKE ? ESCAPE '!' ORDER BY d.created_at"
            + " DESC,d.id LIMIT ? OFFSET ?",
        cutoff,
        cutoff,
        cutoff,
        organizationId,
        namePattern,
        size,
        offset);
  }

  @Override
  public Mono<Long> countByNamePattern(String organizationId, String namePattern) {
    return count(
        "SELECT COUNT(*) FROM devices WHERE organization_id=? AND name LIKE ? ESCAPE '!'",
        organizationId,
        namePattern);
  }

  @Override
  public Mono<DeviceStatsView> stats(String organizationId, long onlineSince) {
    return one(
        DeviceStatsView.class,
        "SELECT COUNT(*) AS total,CAST(COALESCE(SUM(status='ACTIVE' AND last_seen_at>=?),0) AS"
            + " SIGNED) AS online,CAST(COALESCE(SUM(status='REVOKED'),0) AS SIGNED) AS revoked"
            + " FROM devices WHERE organization_id=?",
        onlineSince,
        organizationId);
  }
}
