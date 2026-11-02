package com.anhtran.securemdm.src.device.repository.impl;

import com.anhtran.securemdm.common.repository.SqlSupport;
import com.anhtran.securemdm.src.device.entity.Device;
import com.anhtran.securemdm.src.device.repository.DeviceCredentialRepositoryCustom;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class DeviceCredentialRepositoryCustomImpl extends SqlSupport
    implements DeviceCredentialRepositoryCustom {
  public DeviceCredentialRepositoryCustomImpl(R2dbcEntityTemplate template) {
    super(template);
  }

  @Override
  public Mono<Device> findActiveDevice(String secretHash, long now) {
    return one(
        Device.class,
        "SELECT d.id,d.organization_id FROM device_credentials c JOIN devices d ON"
            + " d.id=c.device_id AND d.organization_id=c.organization_id WHERE c.secret_hash=? AND"
            + " c.revoked_at IS NULL AND c.expires_at>? AND d.status='ACTIVE'",
        secretHash,
        now);
  }

  @Override
  public Mono<Long> revokeForDevice(String organizationId, String deviceId, long at) {
    return execute(
        "UPDATE device_credentials SET revoked_at=? WHERE organization_id=? AND device_id=?",
        at,
        organizationId,
        deviceId);
  }
}
