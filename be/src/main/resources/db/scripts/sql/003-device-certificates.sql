-- Device identity is an X.509 certificate for a device-held key (ADR 0005).
CREATE TABLE device_certificates (
 id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, device_id VARCHAR(36) NOT NULL,
 serial VARCHAR(40) NOT NULL UNIQUE, public_key_jwk JSON NOT NULL, fingerprint CHAR(64) NOT NULL,
 certificate_pem TEXT NOT NULL, not_before BIGINT NOT NULL, not_after BIGINT NOT NULL, revoked_at BIGINT,
 FOREIGN KEY(organization_id,device_id) REFERENCES devices(organization_id,id));
CREATE INDEX device_certificates_device ON device_certificates(organization_id,device_id);
-- One row per used Device Proof jti, kept until the proof's iat + 60 s (it is rejected after that anyway).
CREATE TABLE device_proof_replays (
 device_id VARCHAR(36) NOT NULL, jti VARCHAR(64) NOT NULL, expires_at BIGINT NOT NULL,
 PRIMARY KEY(device_id,jti));
CREATE INDEX device_proof_replays_expiry ON device_proof_replays(expires_at);
