import { afterEach, expect, it, vi } from 'vitest';
import { generateKeyPair, exportJWK, SignJWT } from 'jose';
import { mkdtemp, writeFile, rm, readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { api, tick, LegacyState } from './agent.js';
import { save, load, type State } from './state.js';
import { newDeviceKey } from './pki.js';
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
      serial: '7f01',
      certificate: 'test-only',
      deviceKey: (await newDeviceKey()).privateJwk,
      expiresAt: 0,
      policyRevision: 0,
      results: {},
      messages: [],
      inventory: {
        screenLockEnabled: false,
        diskEncrypted: false,
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
                requireDiskEncryption: true,
                minAndroidApiLevel: 36,
                blockedPackages: ['com.blocked'],
              },
            },
          };
        if (url.endsWith('/policy-report')) {
          const report = JSON.parse(String(options.body));
          expect(report.revision).toBe(2);
          expect(report.inventory.screenLockEnabled).toBe(true);
          expect(report.inventory.diskEncrypted).toBe(true);
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
it('follows command-key rotation on heartbeat and keeps working when it cannot', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'mdm-trust-'));
  try {
    const one = await generateKeyPair('ES256');
    const two = await generateKeyPair('ES256');
    const keys = [
      { ...(await exportJWK(one.publicKey)), kid: 'command-1' },
      { ...(await exportJWK(two.publicKey)), kid: 'command-2' },
    ];
    const anchor = join(dir, 'provisioned.json');
    await writeFile(anchor, JSON.stringify({ keys: [keys[0]] }));
    await save(dir, {
      deviceId: 'device',
      organizationId: 'tenant',
      serial: '7f01',
      certificate: 'test-only',
      deviceKey: (await newDeviceKey()).privateJwk,
      expiresAt: 0,
      policyRevision: 0,
      results: {},
      messages: [],
      inventory: {
        screenLockEnabled: false,
        diskEncrypted: false,
        androidApiLevel: 35,
        installedPackages: [],
      },
    });
    const bundle = await new SignJWT({ keys })
      .setProtectedHeader({ alg: 'ES256', typ: 'mdm-trust+jwt', kid: 'command-1' })
      .setIssuer('secure-mdm')
      .setIssuedAt()
      .setExpirationTime('1d')
      .sign(one.privateKey);
    let keysStatus = 500;
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, options: RequestInit) => {
        expect(String((options.headers as Record<string, string>).Authorization)).toMatch(
          /^DeviceProof ey/
        );
        if (url.endsWith('/command-keys'))
          return new Response(JSON.stringify({ bundles: [bundle] }), { status: keysStatus });
        if (url.endsWith('/policy')) return new Response(JSON.stringify({ revision: 0 }));
        if (url.endsWith('/claim')) return new Response(null, { status: 204 });
        return new Response(null, { status: 200 });
      })
    );
    vi.spyOn(console, 'warn').mockImplementation(() => {});
    await expect(tick('http://127.0.0.1:8080', dir, anchor, { heartbeat: true })).resolves.toBe(
      false
    );
    await expect(readFile(join(dir, 'trust.json'), 'utf8')).rejects.toThrow();
    keysStatus = 200;
    await tick('http://127.0.0.1:8080', dir, anchor, { heartbeat: true });
    const trust = JSON.parse(await readFile(join(dir, 'trust.json'), 'utf8'));
    expect(trust.keys.map((k: { kid: string }) => k.kid)).toEqual(['command-1', 'command-2']);
  } finally {
    await rm(dir, { recursive: true });
  }
});
it('stops with a clear message on state enrolled before device certificates', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'mdm-legacy-'));
  try {
    await writeFile(
      join(dir, 'state.json'),
      JSON.stringify({ deviceId: 'd', organizationId: 't', credential: 'old', expiresAt: 0 })
    );
    const fetchSpy = vi.fn();
    vi.stubGlobal('fetch', fetchSpy);
    await expect(
      tick('http://127.0.0.1:8080', dir, join(dir, 'trust.json'), { heartbeat: true })
    ).rejects.toBeInstanceOf(LegacyState);
    expect(fetchSpy).not.toHaveBeenCalled();
  } finally {
    await rm(dir, { recursive: true });
  }
});
