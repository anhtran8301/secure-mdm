import type { DeviceCertificate } from 'src/types/api';

/** Mirrors the backend check: usable only while not revoked and before notAfter. */
export function certificateStatus(certificate: DeviceCertificate, now = Date.now()) {
  if (certificate.revoked) return 'REVOKED';
  return now < certificate.notAfter ? 'ACTIVE' : 'EXPIRED';
}
