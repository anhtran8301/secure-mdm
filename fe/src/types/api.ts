/** Backend contracts (be/src/main/java/com/anhtran/securemdm/dto). */
export type Role = 'ORG_ADMIN' | 'OPERATOR' | 'VIEWER';

export type User = {
  id: string;
  organizationId: string;
  organizationSlug: string;
  role: Role;
};

export type Page<T> = { items: T[]; total: number; page: number; size: number };

export type PageParams = { page: number; size?: number };

export type Device = {
  id: string;
  name: string;
  deviceUid: string;
  status: 'ACTIVE' | 'REVOKED';
  connectivity: string;
  compliance: string;
  lastSeenAt: number | null;
  inventory: Record<string, unknown>;
  assignmentRevision: number;
  /** Only on the detail endpoint. */
  certificate?: DeviceCertificate | null;
};

export type DeviceCertificate = {
  serial: string;
  fingerprint: string;
  notAfter: number;
  revoked: boolean;
};

export type CommandType =
  | 'REPORT_STATUS'
  | 'SYNC_DEVICE'
  | 'SHOW_MESSAGE'
  | 'LOCK_DEVICE_SIMULATED';

export type Command = {
  id: string;
  deviceId: string;
  type: CommandType;
  status: string;
  attempts: number;
  createdAt: number;
  expiresAt: number;
  result: Record<string, unknown> | null;
};

export type PolicyConfig = {
  requireScreenLock: boolean;
  requireDiskEncryption: boolean;
  minAndroidApiLevel: number;
  blockedPackages: string[];
};

export type Policy = {
  id: string;
  name: string;
  versionId: string;
  version: number;
  config: PolicyConfig;
};

export type PolicyVersion = { id: string; policyId: string; version: number; config: PolicyConfig };

export type Assignment = { revision: number; policy: PolicyVersion | null };

export type AuditEntry = {
  id: string;
  actorId: string;
  actorType: string;
  action: string;
  resourceId: string;
  resourceType: string;
  correlationId: string;
  createdAt: number;
};

export type Overview = {
  devices: { total: number; online: number; revoked: number };
  commands: { status: string; total: number }[];
};

export type EnrollmentToken = { id: string; token: string; expiresAt: number };
