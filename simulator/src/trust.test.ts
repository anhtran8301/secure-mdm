import { describe, expect, it } from 'vitest';
import { exportJWK, generateKeyPair, SignJWT, type JWK } from 'jose';
import { readFile } from 'node:fs/promises';
import { refreshTrust, type TrustStore } from './trust.js';
import { verifier } from './commands.js';

async function key(kid: string) {
  const { publicKey, privateKey } = await generateKeyPair('ES256');
  return { kid, privateKey, jwk: { ...(await exportJWK(publicKey)), kid, alg: 'ES256' } as JWK };
}
type Key = Awaited<ReturnType<typeof key>>;
const NOW = new Date('2030-01-01T00:00:00Z');
const at = (seconds: number) => Math.floor(NOW.getTime() / 1000) + seconds;

function bundle(signer: Key, keys: JWK[], iat = at(0), type = 'mdm-trust+jwt') {
  return new SignJWT({ keys })
    .setProtectedHeader({ alg: 'ES256', typ: type, kid: signer.kid })
    .setIssuer('secure-mdm')
    .setIssuedAt(iat)
    .setExpirationTime(iat + 86400)
    .sign(signer.privateKey);
}
const store = (...keys: Key[]): TrustStore => ({ keys: keys.map((k) => k.jwk) });
const kids = (trust: TrustStore | null) => trust?.keys.map((k) => k.kid);

describe('trust refresh', async () => {
  const one = await key('command-1');
  const two = await key('command-2');
  const stranger = await key('command-1');

  it('learns the published keys from a bundle signed by a trusted key', async () => {
    const next = await refreshTrust(store(one), [await bundle(one, [one.jwk, two.jwk])], NOW);
    expect(kids(next)).toEqual(['command-1', 'command-2']);
    expect(next?.iat).toBe(at(0));
  });

  it('rejects bundles signed by an unknown key, even one reusing a trusted kid', async () => {
    expect(await refreshTrust(store(one), [await bundle(two, [two.jwk])], NOW)).toBeNull();
    expect(
      await refreshTrust(store(one), [await bundle(stranger, [stranger.jwk])], NOW)
    ).toBeNull();
  });

  it('catches up after a missed publish through the bundle the old key signed', async () => {
    const keys = [one.jwk, two.jwk];
    const bundles = [await bundle(two, keys), await bundle(one, keys)];
    expect(kids(await refreshTrust(store(one), bundles, NOW))).toEqual(['command-1', 'command-2']);
  });

  it('still verifies a command the old key signed before the switch', async () => {
    const command = await new SignJWT({ tenant: 't', commandType: 'SYNC_DEVICE', payload: {} })
      .setProtectedHeader({ alg: 'ES256', typ: 'mdm-command+jwt', kid: one.kid })
      .setIssuer('secure-mdm')
      .setAudience('d')
      .setJti('c')
      .setIssuedAt(at(-60))
      .setExpirationTime(at(540))
      .sign(one.privateKey);
    const next = await refreshTrust(
      store(one),
      [await bundle(two, [one.jwk, two.jwk]), await bundle(one, [one.jwk, two.jwk])],
      NOW
    );
    const { jwtVerify } = await import('jose');
    await expect(jwtVerify(command, verifier(next!), { currentDate: NOW })).resolves.toBeTruthy();
  });

  it('rejects rollback, private keys, empty sets and the wrong type', async () => {
    expect(await refreshTrust(store(one), [await bundle(one, [one.jwk], at(61))], NOW)).toBeNull();
    expect(
      kids(await refreshTrust(store(one), [await bundle(one, [one.jwk], at(60))], NOW))
    ).toEqual(['command-1']);
    const newer = { ...store(one), iat: at(10) };
    expect(await refreshTrust(newer, [await bundle(one, [one.jwk])], NOW)).toBeNull();
    const leaked = { ...one.jwk, d: 'secret' };
    expect(await refreshTrust(store(one), [await bundle(one, [leaked])], NOW)).toBeNull();
    expect(await refreshTrust(store(one), [await bundle(one, [])], NOW)).toBeNull();
    expect(
      await refreshTrust(store(one), [await bundle(one, [one.jwk], at(0), 'mdm-command+jwt')], NOW)
    ).toBeNull();
  });

  it('accepts Java Nimbus trust bundles', async () => {
    const fixture = JSON.parse(
      await readFile(process.env.JAVA_FIXTURE || 'fixtures/java-commands.json', 'utf8')
    );
    const old = { keys: [fixture.jwks.keys[0]] };
    const next = await refreshTrust(old, fixture.bundles, new Date(fixture.at));
    expect(kids(next)).toEqual(fixture.jwks.keys.map((k: JWK) => k.kid));
  });
});
