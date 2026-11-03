-- The bearer device credential is replaced by device certificates (ADR 0005). Devices enrolled
-- before have no certificate and can never authenticate again: revoke them as an admin revoke
-- would (open commands closed), so the console does not show them as healthy. They re-enroll
-- with a new UID. No audit rows: this is a schema migration, not an actor's action.
UPDATE device_commands SET status=CASE WHEN attempts=0 THEN 'CANCELLED' ELSE 'UNKNOWN' END,
 lease_until=NULL, current_attempt_id=NULL
 WHERE status IN ('PENDING','IN_PROGRESS') AND device_id IN (
  SELECT id FROM devices WHERE status='ACTIVE' AND id NOT IN (SELECT device_id FROM device_certificates));
UPDATE devices SET status='REVOKED'
 WHERE status='ACTIVE' AND id NOT IN (SELECT device_id FROM device_certificates);
DROP TABLE device_credentials;
