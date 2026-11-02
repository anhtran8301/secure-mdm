import { readFile, stat } from 'node:fs/promises';
import { join } from 'node:path';
import { load, save, type State } from './state.js';
import { execute, verifier } from './commands.js';

export class ApiError extends Error {
  constructor(public status: number) {
    super(`API request failed: HTTP ${status}`);
  }
}
export class InterruptedAfterResult extends Error {}
export function api(base: string, credential?: string) {
  const url = new URL(base);
  if (
    url.protocol !== 'https:' &&
    !(url.protocol === 'http:' && ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname))
  )
    throw new Error('Use HTTPS except for loopback development');
  return async (path: string, body?: unknown) => {
    const response = await fetch(`${base.replace(/\/$/, '')}/api/v1${path}`, {
      method: body === undefined ? 'GET' : 'POST',
      redirect: 'error',
      signal: AbortSignal.timeout(10000),
      headers: {
        'Content-Type': 'application/json',
        ...(credential ? { Authorization: `Bearer ${credential}` } : {}),
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
  const identity = await api(base)('/device-enrollments', {
    token,
    deviceUid: uid,
    name,
  });
  const state: State = {
    ...identity,
    inventory: {
      screenLockEnabled: false,
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
export async function tick(
  base: string,
  dir: string,
  trustFile: string,
  options: { heartbeat: boolean; crash?: boolean; fail?: boolean }
) {
  const state = await load(dir);
  const request = api(base, state.credential);
  if (options.heartbeat) {
    await synchronize(state, request);
    await save(dir, state);
  }
  const claim = await request('/device-agent/commands/claim', {});
  if (!claim) return false;
  const keys = verifier(JSON.parse(await readFile(trustFile, 'utf8')));
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
