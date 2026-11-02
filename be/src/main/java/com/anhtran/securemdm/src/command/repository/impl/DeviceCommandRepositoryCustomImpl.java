package com.anhtran.securemdm.src.command.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.command.entity.DeviceCommand;
import com.anhtran.securemdm.src.command.repository.DeviceCommandRepositoryCustom;
import com.anhtran.securemdm.src.command.repository.projection.CommandStatusCountView;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class DeviceCommandRepositoryCustomImpl extends SqlSupport
    implements DeviceCommandRepositoryCustom {
  public DeviceCommandRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Flux<DeviceCommand> findOpenForDevice(String organizationId, String deviceId) {
    return all(
        DeviceCommand.class,
        "SELECT * FROM device_commands WHERE organization_id=? AND device_id=? AND status IN"
            + " ('PENDING','IN_PROGRESS') ORDER BY created_at,id",
        organizationId,
        deviceId);
  }

  @Override
  public Mono<Long> transition(String organizationId, String id, String status) {
    return execute(
        "UPDATE device_commands SET status=?,lease_until=NULL,current_attempt_id=NULL WHERE id=?"
            + " AND organization_id=?",
        status,
        id,
        organizationId);
  }

  @Override
  public Mono<Long> countInProgress(String organizationId, String deviceId) {
    return count(
        "SELECT COUNT(*) FROM device_commands WHERE organization_id=? AND device_id=? AND"
            + " status='IN_PROGRESS'",
        organizationId,
        deviceId);
  }

  @Override
  public Mono<DeviceCommand> findNextPending(String organizationId, String deviceId) {
    return one(
        DeviceCommand.class,
        "SELECT * FROM device_commands WHERE organization_id=? AND device_id=? AND"
            + " status='PENDING' ORDER BY queue_order LIMIT 1",
        organizationId,
        deviceId);
  }

  @Override
  public Mono<Long> markClaimed(
      String organizationId, String id, String attemptId, long leaseUntil) {
    return execute(
        "UPDATE device_commands SET status='IN_PROGRESS',attempts=attempts+1,"
            + "current_attempt_id=?,lease_until=? WHERE id=? AND organization_id=?",
        attemptId,
        leaseUntil,
        id,
        organizationId);
  }

  @Override
  public Mono<Long> complete(
      String organizationId, String id, String status, String resultJson, long completedAt) {
    return execute(
        "UPDATE device_commands SET status=?,result_json=?,completed_at=? WHERE id=? AND"
            + " organization_id=?",
        status,
        resultJson,
        completedAt,
        id,
        organizationId);
  }

  @Override
  public Mono<Long> cancelForDevice(String organizationId, String deviceId) {
    return execute(
        "UPDATE device_commands SET status=CASE WHEN attempts=0 THEN 'CANCELLED' ELSE 'UNKNOWN'"
            + " END,lease_until=NULL,current_attempt_id=NULL WHERE organization_id=? AND"
            + " device_id=? AND status IN ('PENDING','IN_PROGRESS')",
        organizationId,
        deviceId);
  }

  @Override
  public Flux<DeviceCommand> findPage(
      String organizationId, String deviceId, int size, long offset) {
    return all(
        DeviceCommand.class,
        "SELECT * FROM device_commands WHERE organization_id=? AND device_id=? ORDER BY"
            + " created_at DESC,id LIMIT ? OFFSET ?",
        organizationId,
        deviceId,
        size,
        offset);
  }

  @Override
  public Flux<DeviceCommand> findDueDevices(long now) {
    return all(
        DeviceCommand.class,
        "SELECT DISTINCT organization_id,device_id FROM device_commands WHERE status IN"
            + " ('PENDING','IN_PROGRESS') AND (expires_at<=? OR lease_until<=?)",
        now,
        now);
  }

  @Override
  public Flux<CommandStatusCountView> countByStatus(String organizationId) {
    return all(
        CommandStatusCountView.class,
        "SELECT status,COUNT(*) AS total FROM device_commands WHERE organization_id=? GROUP BY"
            + " status",
        organizationId);
  }
}
