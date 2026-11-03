import { mkdir, open, readFile, rename, chmod } from 'node:fs/promises';
import { join } from 'node:path';
import lockfile from 'proper-lockfile';
import type { JWK } from 'jose';

export type Inventory = {
  screenLockEnabled: boolean;
  diskEncrypted: boolean;
  androidApiLevel: number;
  installedPackages: string[];
};
export type Outcome = {
  digest: string;
  status: 'SUCCEEDED' | 'FAILED';
  result: Record<string, unknown>;
};
export type State = {
  deviceId: string;
  organizationId: string;
  /** Certificate serial, sent as the kid of every Device Proof. */
  serial: string;
  certificate: string;
  /** Private device key; the state file is 0600 and never logged. */
  deviceKey: JWK;
  expiresAt: number;
  inventory: Inventory;
  results: Record<string, Outcome>;
  messages: { commandId: string; message: string }[];
  policyRevision: number;
};
export async function withLock<T>(dir: string, work: () => Promise<T>): Promise<T> {
  await mkdir(dir, { recursive: true, mode: 0o700 });
  const release = await lockfile.lock(dir, {
    retries: 0,
    stale: 10000,
    update: 2000,
  });
  try {
    return await work();
  } finally {
    await release();
  }
}
export async function load(dir: string): Promise<State> {
  return JSON.parse(await readFile(join(dir, 'state.json'), 'utf8'));
}
export async function save(dir: string, state: State): Promise<void> {
  await writeJson(dir, 'state.json', state);
}
/** Atomic, owner-only write: temp file, fsync, rename, fsync the directory. */
export async function writeJson(dir: string, name: string, value: unknown): Promise<void> {
  const temp = join(dir, `${name}.tmp`);
  const file = await open(temp, 'w', 0o600);
  try {
    await file.writeFile(JSON.stringify(value));
    await file.sync();
  } finally {
    await file.close();
  }
  await chmod(temp, 0o600);
  await rename(temp, join(dir, name));
  const directory = await open(dir, 'r');
  try {
    await directory.sync();
  } finally {
    await directory.close();
  }
}
