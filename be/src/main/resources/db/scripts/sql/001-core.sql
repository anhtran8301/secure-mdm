CREATE TABLE organizations (id VARCHAR(36) PRIMARY KEY, slug VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(120) NOT NULL);
CREATE TABLE users (
 id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, email VARCHAR(180) NOT NULL,
 password_hash VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 UNIQUE(organization_id,email), UNIQUE(organization_id,id), FOREIGN KEY(organization_id) REFERENCES organizations(id));
CREATE TABLE auth_sessions (
 id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, user_id VARCHAR(36) NOT NULL,
 expires_at BIGINT NOT NULL, revoked_at BIGINT,
 FOREIGN KEY(organization_id,user_id) REFERENCES users(organization_id,id));
CREATE TABLE refresh_tokens (token_hash CHAR(64) PRIMARY KEY, session_id VARCHAR(36) NOT NULL,
 used_at BIGINT, FOREIGN KEY(session_id) REFERENCES auth_sessions(id));
CREATE TABLE enrollment_tokens (
 id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, token_hash CHAR(64) NOT NULL UNIQUE,
 expires_at BIGINT NOT NULL, used_at BIGINT, revoked_at BIGINT, created_at BIGINT NOT NULL,
 FOREIGN KEY(organization_id) REFERENCES organizations(id));
CREATE TABLE devices (
 id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, device_uid VARCHAR(100) NOT NULL,
 name VARCHAR(120) NOT NULL, platform VARCHAR(30) NOT NULL DEFAULT 'ANDROID_SIMULATED',
 status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', last_seen_at BIGINT, created_at BIGINT NOT NULL,
 inventory_json JSON NOT NULL, assignment_revision BIGINT NOT NULL DEFAULT 0,
 UNIQUE(organization_id,id), UNIQUE(organization_id,device_uid),
 FOREIGN KEY(organization_id) REFERENCES organizations(id));
CREATE TABLE device_credentials (
 id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, device_id VARCHAR(36) NOT NULL,
 secret_hash CHAR(64) NOT NULL UNIQUE, expires_at BIGINT NOT NULL, revoked_at BIGINT,
 FOREIGN KEY(organization_id,device_id) REFERENCES devices(organization_id,id));
CREATE TABLE policies (
 id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, name VARCHAR(120) NOT NULL,
 created_at BIGINT NOT NULL, UNIQUE(organization_id,id), FOREIGN KEY(organization_id) REFERENCES organizations(id));
CREATE TABLE policy_versions (
 id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, policy_id VARCHAR(36) NOT NULL,
 version INT NOT NULL, config_json JSON NOT NULL, created_at BIGINT NOT NULL,
 UNIQUE(organization_id,policy_id,version), UNIQUE(organization_id,id),
 FOREIGN KEY(organization_id,policy_id) REFERENCES policies(organization_id,id));
CREATE TABLE device_policy_assignments (
 device_id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, version_id VARCHAR(36) NOT NULL,
 revision BIGINT NOT NULL, assigned_at BIGINT NOT NULL,
 FOREIGN KEY(organization_id,device_id) REFERENCES devices(organization_id,id),
 FOREIGN KEY(organization_id,version_id) REFERENCES policy_versions(organization_id,id));
CREATE TABLE device_policy_reports (
 device_id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, revision BIGINT NOT NULL,
 version_id VARCHAR(36) NOT NULL, compliance VARCHAR(20) NOT NULL, reasons_json JSON NOT NULL, reported_at BIGINT NOT NULL,
 FOREIGN KEY(organization_id,device_id) REFERENCES devices(organization_id,id),
 FOREIGN KEY(organization_id,version_id) REFERENCES policy_versions(organization_id,id));
CREATE TABLE device_commands (
 id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, device_id VARCHAR(36) NOT NULL,
 created_by VARCHAR(36) NOT NULL, type VARCHAR(40) NOT NULL, payload_json JSON NOT NULL,
 request_hash CHAR(64) NOT NULL, idempotency_key VARCHAR(100) NOT NULL, status VARCHAR(20) NOT NULL,
 signed_command TEXT NOT NULL, attempts INT NOT NULL DEFAULT 0, current_attempt_id VARCHAR(36),
 lease_until BIGINT, created_at BIGINT NOT NULL, expires_at BIGINT NOT NULL, completed_at BIGINT,
 result_json JSON, UNIQUE(organization_id,created_by,idempotency_key), UNIQUE(organization_id,id),
 INDEX command_queue(organization_id,device_id,status,created_at),
 FOREIGN KEY(organization_id,device_id) REFERENCES devices(organization_id,id));
CREATE TABLE command_attempts (
 id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, command_id VARCHAR(36) NOT NULL,
 claimed_at BIGINT NOT NULL, lease_until BIGINT NOT NULL,
 FOREIGN KEY(organization_id,command_id) REFERENCES device_commands(organization_id,id));
CREATE TABLE audit_logs (
 id VARCHAR(36) PRIMARY KEY, organization_id VARCHAR(36) NOT NULL, actor_type VARCHAR(20) NOT NULL,
 actor_id VARCHAR(36) NOT NULL, action VARCHAR(80) NOT NULL, resource_type VARCHAR(40) NOT NULL,
 resource_id VARCHAR(36) NOT NULL, correlation_id VARCHAR(36) NOT NULL, created_at BIGINT NOT NULL,
 INDEX audit_tenant_time(organization_id,created_at), FOREIGN KEY(organization_id) REFERENCES organizations(id));
