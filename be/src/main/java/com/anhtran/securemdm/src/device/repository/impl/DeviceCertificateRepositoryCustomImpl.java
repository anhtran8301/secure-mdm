package com.anhtran.securemdm.src.device.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.device.entity.DeviceCertificate;
import com.anhtran.securemdm.src.device.repository.DeviceCertificateRepositoryCustom;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class DeviceCertificateRepositoryCustomImpl extends SqlSupport
    implements DeviceCertificateRepositoryCustom {
  public DeviceCertificateRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Mono<DeviceCertificate> findUsable(String serial, long now) {
    return one(
        DeviceCertificate.class,
        "SELECT c.* FROM device_certificates c JOIN devices d ON d.id=c.device_id AND"
            + " d.organization_id=c.organization_id WHERE c.serial=? AND c.revoked_at IS NULL AND"
            + " c.not_before<=? AND c.not_after>? AND d.status='ACTIVE'",
        serial,
        now,
        now);
  }

  @Override
  public Mono<DeviceCertificate> findLatest(String organizationId, String deviceId) {
    return one(
        DeviceCertificate.class,
        "SELECT * FROM device_certificates WHERE organization_id=? AND device_id=?"
            + " ORDER BY not_before DESC, id DESC LIMIT 1",
        organizationId,
        deviceId);
  }

  @Override
  public Mono<Long> revokeForDevice(String organizationId, String deviceId, long at) {
    return execute(
        "UPDATE device_certificates SET revoked_at=? WHERE organization_id=? AND device_id=?"
            + " AND revoked_at IS NULL",
        at,
        organizationId,
        deviceId);
  }
}
