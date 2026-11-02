import { afterEach, expect, it, vi } from 'vitest';
import { generateKeyPair, exportJWK, SignJWT } from 'jose';
import { mkdtemp, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { api, tick } from './agent.js';
import { save, load, type State } from './state.js';
afterEach(() => vi.unstubAllGlobals());
it('SYNC_DEVICE applies current policy immediately outside the heartbeat interval', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'mdm-sync-'));
  try {
    const { publicKey, privateKey } = await generateKeyPair('ES256');
    const trust = join(dir, 'trust.json');
    await writeFile(
      trust,
      JSON.stringify({
        keys: [{ ...(await exportJWK(publicKey)), kid: 'test' }],
      })
    );
    const state: State = {
      deviceId: 'device',
      organizationId: 'tenant',
      credential: 'test-only',
      expiresAt: 0,
      policyRevision: 0,
      results: {},
      messages: [],
      inventory: {
        screenLockEnabled: false,
        androidApiLevel: 35,
        installedPackages: ['com.blocked'],
      },
    };
    await save(dir, state);
    const signed = await new SignJWT({
      tenant: 'tenant',
      commandType: 'SYNC_DEVICE',
      payload: {},
    })
      .setProtectedHeader({ alg: 'ES256', typ: 'mdm-command+jwt', kid: 'test' })
      .setIssuer('secure-mdm')
      .setAudience('device')
      .setJti('sync')
      .setIssuedAt()
      .setExpirationTime('10m')
      .sign(privateKey);
    let reported = false;
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, options: RequestInit) => {
        let body: unknown = null;
        if (url.endsWith('/claim'))
          body = {
            commandId: 'sync',
            attemptId: 'attempt',
            signedCommand: signed,
          };
        if (url.endsWith('/policy'))
          body = {
            revision: 2,
            policy: {
              id: 'v2',
              config: {
                requireScreenLock: true,
                minAndroidApiLevel: 36,
                blockedPackages: ['com.blocked'],
              },
            },
          };
        if (url.endsWith('/policy-report')) {
          const report = JSON.parse(String(options.body));
          expect(report.revision).toBe(2);
          expect(report.inventory.screenLockEnabled).toBe(true);
          expect(report.inventory.installedPackages).toEqual([]);
          expect(report.inventory.androidApiLevel).toBe(35);
          reported = true;
        }
        return new Response(body ? JSON.stringify(body) : null, {
          status: 200,
        });
      })
    );
    await tick('http://127.0.0.1:8080', dir, trust, { heartbeat: false });
    expect(reported).toBe(true);
    const persisted = await load(dir);
    expect(persisted.policyRevision).toBe(2);
    expect(persisted.results.sync.status).toBe('SUCCEEDED');
    expect(persisted.inventory.screenLockEnabled).toBe(true);
  } finally {
    await rm(dir, { recursive: true });
  }
});
it('refuses plaintext HTTP outside loopback', () => {
  expect(() => api('http://example.com')).toThrow('HTTPS');
  expect(() => api('https://example.com')).not.toThrow();
});
