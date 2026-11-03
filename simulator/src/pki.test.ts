import 'reflect-metadata';
import * as x509 from '@peculiar/x509';
import { describe, expect, it } from 'vitest';
import { decodeProtectedHeader, importJWK, jwtVerify } from 'jose';
import { checkIssued, newDeviceKey, proof } from './pki.js';

const ALG = { name: 'ECDSA', namedCurve: 'P-256', hash: 'SHA-256' };

async function ca() {
  const keys = await crypto.subtle.generateKey(ALG, true, ['sign', 'verify']);
  const cert = await x509.X509CertificateGenerator.createSelfSigned({
    name: 'CN=Test CA',
    keys,
    signingAlgorithm: ALG,
    extensions: [new x509.BasicConstraintsExtension(true, 0, true)],
  });
  return { keys, cert };
}
async function issue(authority: Awaited<ReturnType<typeof ca>>, csrPem: string) {
  const csr = new x509.Pkcs10CertificateRequest(csrPem);
  return x509.X509CertificateGenerator.create({
    serialNumber: '7f01',
    subject: 'O=tenant,CN=device',
    issuer: authority.cert.subject,
    notBefore: new Date(Date.now() - 1000),
    notAfter: new Date(Date.now() + 86400000),
    publicKey: csr.publicKey,
    signingKey: authority.keys.privateKey,
    signingAlgorithm: ALG,
  });
}

describe('device key', async () => {
  const authority = await ca();
  const device = await newDeviceKey();

  it('makes a self-signed P-256 CSR and keeps the private key out of it', async () => {
    const csr = new x509.Pkcs10CertificateRequest(device.csr);
    expect(await csr.verify()).toBe(true);
    expect(device.csr).not.toContain(device.privateJwk.d);
    expect(device.privateJwk).toMatchObject({ kty: 'EC', crv: 'P-256' });
  });

  it('accepts only its own certificate issued by the returned CA', async () => {
    const cert = (await issue(authority, device.csr)).toString('pem');
    await expect(
      checkIssued(cert, authority.cert.toString('pem'), device.privateJwk)
    ).resolves.toBeUndefined();
    const other = await newDeviceKey();
    const notMine = (await issue(authority, other.csr)).toString('pem');
    await expect(
      checkIssued(notMine, authority.cert.toString('pem'), device.privateJwk)
    ).rejects.toThrow();
    const stranger = await ca();
    await expect(
      checkIssued(cert, stranger.cert.toString('pem'), device.privateJwk)
    ).rejects.toThrow();
  });

  it('signs a Device Proof for one request', async () => {
    const now = new Date('2030-01-01T00:00:00Z');
    const signed = await proof(
      device.privateJwk,
      '7f01',
      'POST',
      '/api/v1/device-agent/heartbeat',
      now
    );
    expect(decodeProtectedHeader(signed)).toMatchObject({
      alg: 'ES256',
      typ: 'mdm-device-proof+jwt',
      kid: '7f01',
    });
    const { d, ...publicJwk } = device.privateJwk;
    const { payload } = await jwtVerify(signed, await importJWK(publicJwk, 'ES256'), {
      currentDate: now,
    });
    expect(payload).toMatchObject({
      htm: 'POST',
      htu: '/api/v1/device-agent/heartbeat',
      iat: now.getTime() / 1000,
    });
    expect(payload.jti).toBeTypeOf('string');
    const again = await proof(
      device.privateJwk,
      '7f01',
      'POST',
      '/api/v1/device-agent/heartbeat',
      now
    );
    expect(
      (await jwtVerify(again, await importJWK(publicJwk, 'ES256'), { currentDate: now })).payload
        .jti
    ).not.toBe(payload.jti);
    expect(d).toBeTypeOf('string');
  });
});
