import { describe, it, expect } from 'vitest';
import { generateKeyPair, exportJWK, SignJWT } from 'jose';
import { mkdtemp, readFile, stat, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { execute, verifier } from './commands.js';
import { save, load, withLock, type State } from './state.js';
const initial = (): State => ({
  deviceId: 'device-test',
  organizationId: 'tenant-test',
  credential: 'test-only',
  expiresAt: 0,
  inventory: {
    screenLockEnabled: false,
    androidApiLevel: 35,
    installedPackages: [],
  },
  results: {},
  messages: [],
  policyRevision: 0,
});
async function signer() {
  const { publicKey, privateKey } = await generateKeyPair('ES256');
  const jwk = { ...(await exportJWK(publicKey)), kid: 'test-key' };
  const keys = verifier({ keys: [jwk] });
  const sign = (claims = {}, header = {}, issuer = 'secure-mdm') =>
    new SignJWT({
      tenant: 'tenant-test',
      commandType: 'SHOW_MESSAGE',
      payload: { message: 'Hello' },
      ...claims,
    })
      .setProtectedHeader({
        alg: 'ES256',
        kid: 'test-key',
        typ: 'mdm-command+jwt',
        ...header,
      })
      .setIssuer(issuer)
      .setAudience('device-test')
      .setJti('command-test')
      .setIssuedAt()
      .setExpirationTime('10m')
      .sign(privateKey);
  return { sign, keys };
}
describe('signed commands and durable dedup', () => {
  it('commits effect and result together; rejects concurrent processes', async () => {
    const dir = await mkdtemp(join(tmpdir(), 'mdm-state-'));
    try {
      const { sign, keys } = await signer();
      const signed = await sign();
      await withLock(dir, async () => {
        await expect(withLock(dir, async () => {})).rejects.toThrow();
        const first = await execute(initial(), signed, keys);
        await save(dir, first.state);
        const recovered = await execute(await load(dir), signed, keys);
        await save(dir, recovered.state);
        expect((await load(dir)).messages).toHaveLength(1);
        expect(recovered.outcome).toEqual(first.outcome);
        expect((await stat(join(dir, 'state.json'))).mode & 0o777).toBe(0o600);
      });
    } finally {
      await rm(dir, { recursive: true });
    }
  });
  it('rejects tamper, wrong tenant/device, expiry, key, typ and algorithm', async () => {
    const { sign, keys } = await signer();
    const signed = await sign();
    const parts = signed.split('.');
    parts[1] = Buffer.from(JSON.stringify({ tenant: 'attacker' })).toString('base64url');
    await expect(execute(initial(), parts.join('.'), keys)).rejects.toThrow();
    await expect(execute({ ...initial(), deviceId: 'wrong' }, signed, keys)).rejects.toThrow();
    await expect(
      execute({ ...initial(), organizationId: 'wrong' }, signed, keys)
    ).rejects.toThrow();
    await expect(
      execute(initial(), signed, keys, false, new Date(Date.now() + 700000))
    ).rejects.toThrow();
    await expect(execute(initial(), await sign({}, { kid: 'unknown' }), keys)).rejects.toThrow();
    await expect(execute(initial(), await sign({}, { typ: 'at+jwt' }), keys)).rejects.toThrow();
    await expect(execute(initial(), await sign({}, {}, 'wrong-issuer'), keys)).rejects.toThrow();
    await expect(execute(initial(), signed, (await signer()).keys)).rejects.toThrow();
    const bad = [
      Buffer.from(JSON.stringify({ alg: 'none', typ: 'mdm-command+jwt' })).toString('base64url'),
      parts[1],
      '',
    ].join('.');
    await expect(execute(initial(), bad, keys)).rejects.toThrow();
    await expect(
      execute(initial(), signed, keys, false, new Date(), 'different-command')
    ).rejects.toThrow();
  });
  it('rejects unsupported payloads and conflicting dedup IDs; persists failures', async () => {
    const { sign, keys } = await signer();
    for (const claims of [
      { commandType: 'SHELL' },
      { payload: { message: '' } },
      { payload: { message: 'Hi', extra: true } },
    ])
      await expect(execute(initial(), await sign(claims), keys)).rejects.toThrow();
    const first = await execute(initial(), await sign(), keys);
    await expect(
      execute(first.state, await sign({ payload: { message: 'Changed' } }), keys)
    ).rejects.toThrow();
    const failure = await execute(initial(), await sign(), keys, true);
    expect(failure.outcome.status).toBe('FAILED');
    expect(failure.state.messages).toHaveLength(0);
    for (const type of ['REPORT_STATUS', 'SYNC_DEVICE', 'LOCK_DEVICE_SIMULATED'])
      expect(
        (await execute(initial(), await sign({ commandType: type, payload: {} }), keys)).outcome
          .status
      ).toBe('SUCCEEDED');
  });
  it('verifies Java Nimbus fixtures across signing-key rotation', async () => {
    const fixture = JSON.parse(
      await readFile(process.env.JAVA_FIXTURE || 'fixtures/java-commands.json', 'utf8')
    );
    let state = initial();
    for (const signed of fixture.commands)
      state = (await execute(state, signed, verifier(fixture.jwks), false, new Date(fixture.at)))
        .state;
    expect(state.messages[0].message).toBe('Java to TypeScript');
    expect(state.inventory.screenLockEnabled).toBe(true);
  });
});
