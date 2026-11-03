import 'reflect-metadata';
import * as x509 from '@peculiar/x509';
import { calculateJwkThumbprint, exportJWK, importJWK, SignJWT, type JWK } from 'jose';
import { randomUUID } from 'node:crypto';

const ALG = { name: 'ECDSA', namedCurve: 'P-256', hash: 'SHA-256' };

/**
 * A software-backed device key (a real agent would keep it in Android Keystore/StrongBox or the
 * Secure Enclave) and its CSR. The server ignores the CSR subject and sets its own.
 */
export async function newDeviceKey(): Promise<{ csr: string; privateJwk: JWK }> {
  const keys = await crypto.subtle.generateKey(ALG, true, ['sign', 'verify']);
  const csr = await x509.Pkcs10CertificateRequestGenerator.create({
    name: 'CN=secure-mdm-device',
    keys,
    signingAlgorithm: ALG,
  });
  return { csr: csr.toString('pem'), privateJwk: await exportJWK(keys.privateKey) };
}

/** Rejects a certificate that is not for this device's key or not signed by the given CA. */
export async function checkIssued(certificatePem: string, caPem: string, privateJwk: JWK) {
  const certificate = new x509.X509Certificate(certificatePem);
  const authority = new x509.X509Certificate(caPem);
  const { d: _, ...own } = privateJwk;
  const issued = await exportJWK(await certificate.publicKey.export());
  if (
    (await calculateJwkThumbprint(issued)) !== (await calculateJwkThumbprint(own)) ||
    certificate.issuer !== authority.subject ||
    !(await certificate.verify({ publicKey: authority.publicKey, signatureOnly: true }))
  )
    throw new Error('Issued certificate does not match this device key or its CA');
}

/** A Device Proof (ADR 0005): binds one request's method and target to the device key. */
export async function proof(
  privateJwk: JWK,
  serial: string,
  method: string,
  target: string,
  now = new Date()
) {
  return new SignJWT({ htm: method, htu: target })
    .setProtectedHeader({ alg: 'ES256', typ: 'mdm-device-proof+jwt', kid: serial })
    .setIssuedAt(Math.floor(now.getTime() / 1000))
    .setJti(randomUUID())
    .sign(await importJWK(privateJwk, 'ES256'));
}
