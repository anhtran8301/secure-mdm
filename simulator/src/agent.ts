import { readFile, stat } from 'node:fs/promises';
import { join } from 'node:path';
import { load, save, writeJson, type State } from './state.js';
import { refreshTrust, type TrustStore } from './trust.js';
import { checkIssued, newDeviceKey, proof } from './pki.js';
import { execute, verifier } from './commands.js';

export class ApiError extends Error {
  constructor(public status: number) {
    super(`API request failed: HTTP ${status}`);
  }
}
export class InterruptedAfterResult extends Error {}
/** State from before device certificates (bearer credential, no device key): cannot authenticate. */
export class LegacyState extends Error {
  constructor() {
    super(
      'This state was enrolled before device certificates and can no longer authenticate. ' +
        'Enroll again with a new --state directory (or an empty volume) and a new --uid.'
    );
  }
}
/** Calls the API; with a device identity, every request carries a fresh Device Proof. */
export function api(base: string, device?: Pick<State, 'deviceKey' | 'serial'>) {
  const url = new URL(base);
  if (
    url.protocol !== 'https:' &&
    !(url.protocol === 'http:' && ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname))
  )
    throw new Error('Use HTTPS except for loopback development');
  return async (path: string, body?: unknown) => {
    const target = `${base.replace(/\/$/, '')}/api/v1${path}`;
    const method = body === undefined ? 'GET' : 'POST';
    const { pathname, search } = new URL(target);
    const authorization = device
      ? `DeviceProof ${await proof(device.deviceKey, device.serial, method, pathname + search)}`
      : undefined;
    const response = await fetch(target, {
      method,
      redirect: 'error',
      signal: AbortSignal.timeout(10000),
      headers: {
        'Content-Type': 'application/json',
        ...(authorization ? { Authorization: authorization } : {}),
      },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    if (!response.ok) throw new ApiError(response.status);
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  };
}
export async function enroll(base: string, dir: string, token: string, uid: string, name: string) {
  try {
    await stat(join(dir, 'state.json'));
    throw new Error('State already enrolled; use a new directory');
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error;
  }
  const { csr, privateJwk } = await newDeviceKey();
  const issued = await api(base)('/device-enrollments', {
    token,
    deviceUid: uid,
    name,
    csr,
  });
  await checkIssued(issued.certificate, issued.caCertificate, privateJwk);
  const state: State = {
    deviceId: issued.deviceId,
    organizationId: issued.organizationId,
    serial: issued.serial,
    certificate: issued.certificate,
    deviceKey: privateJwk,
    expiresAt: issued.expiresAt,
    inventory: {
      screenLockEnabled: false,
      diskEncrypted: false,
      androidApiLevel: 35,
      installedPackages: ['com.example.browser'],
    },
    results: {},
    messages: [],
    policyRevision: 0,
  };
  await save(dir, state);
  return state.deviceId;
}
async function synchronize(state: State, request: ReturnType<typeof api>) {
  const assignment = await request('/device-agent/policy');
  if (assignment.policy) {
    const config = assignment.policy.config;
    if (config.requireScreenLock) state.inventory.screenLockEnabled = true;
    // Simulated FileVault/FBE: a real agent would ask the OS to encrypt and report its status.
    if (config.requireDiskEncryption) state.inventory.diskEncrypted = true;
    state.inventory.installedPackages = state.inventory.installedPackages.filter(
      (p) => !config.blockedPackages.includes(p)
    );
    state.policyRevision = assignment.revision;
    try {
      await request('/device-agent/policy-report', {
        revision: assignment.revision,
        versionId: assignment.policy.id,
        inventory: state.inventory,
      });
    } catch (error) {
      if (!(error instanceof ApiError && error.status === 409)) throw error;
    }
  } else {
    state.policyRevision = assignment.revision;
  }
  await request('/device-agent/heartbeat', state.inventory);
}
/** The provisioned trust file is the anchor; refreshed trust lives in the writable state dir. */
async function readTrust(dir: string, trustFile: string): Promise<TrustStore> {
  try {
    return JSON.parse(await readFile(join(dir, 'trust.json'), 'utf8'));
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error;
    return JSON.parse(await readFile(trustFile, 'utf8'));
  }
}
async function followKeyRotation(dir: string, trustFile: string, request: ReturnType<typeof api>) {
  try {
    const { bundles } = await request('/device-agent/command-keys');
    const next = await refreshTrust(await readTrust(dir, trustFile), bundles);
    if (next) await writeJson(dir, 'trust.json', next);
    else console.warn('No trust bundle verified; keeping current command keys');
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) throw error;
    console.warn('Trust refresh failed; keeping current command keys');
  }
}
export async function tick(
  base: string,
  dir: string,
  trustFile: string,
  options: { heartbeat: boolean; crash?: boolean; fail?: boolean }
) {
  const state = await load(dir);
  if (!state.deviceKey || !state.serial) throw new LegacyState();
  const request = api(base, state);
  if (options.heartbeat) {
    await synchronize(state, request);
    await save(dir, state);
    await followKeyRotation(dir, trustFile, request);
  }
  const claim = await request('/device-agent/commands/claim', {});
  if (!claim) return false;
  const keys = verifier(await readTrust(dir, trustFile));
  const execution = await execute(
    state,
    claim.signedCommand,
    keys,
    options.fail,
    new Date(),
    claim.commandId
  );
  if (
    execution.commandType === 'SYNC_DEVICE' &&
    execution.outcome.status === 'SUCCEEDED' &&
    !state.results[claim.commandId]
  ) {
    await synchronize(execution.state, request);
  }
  // Persist effect and dedup result together, before acknowledging to the server.
  await save(dir, execution.state);
  if (options.crash)
    throw new InterruptedAfterResult('Stopped after durable result, before acknowledgement');
  try {
    await request(`/device-agent/commands/${claim.commandId}/result`, {
      attemptId: claim.attemptId,
      status: execution.outcome.status,
      result: execution.outcome.result,
    });
  } catch (error) {
    if (!(error instanceof ApiError && error.status === 409)) throw error;
  }
  return true;
}
