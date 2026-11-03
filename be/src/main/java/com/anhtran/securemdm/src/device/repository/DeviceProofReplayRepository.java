package com.anhtran.securemdm.src.device.repository;

import com.anhtran.securemdm.common.repository.SqlSupport;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/** Used Device Proof ids; the primary key makes a replayed (device, jti) fail to insert. */
@Repository
public class DeviceProofReplayRepository extends SqlSupport {
  public DeviceProofReplayRepository(R2dbcEntityTemplate template) {
    super(template);
  }

  /** Errors with a DataIntegrityViolationException when the jti was already used. */
  public Mono<Long> remember(String deviceId, String jti, long expiresAt) {
    return execute(
        "INSERT INTO device_proof_replays(device_id,jti,expires_at) VALUES (?,?,?)",
        deviceId,
        jti,
        expiresAt);
  }

  public Mono<Long> purgeExpired(long now) {
    return execute("DELETE FROM device_proof_replays WHERE expires_at<?", now);
  }
}
