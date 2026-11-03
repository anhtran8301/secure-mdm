import { it, expect, describe } from 'vitest';

import { certificateStatus } from './certificate';

const cert = { serial: 'ab', fingerprint: 'f', notAfter: 1_000, revoked: false };

describe('certificateStatus', () => {
  it('is active until notAfter, then expired', () => {
    expect(certificateStatus(cert, 999)).toBe('ACTIVE');
    expect(certificateStatus(cert, 1_000)).toBe('EXPIRED');
  });

  it('shows revoked first', () => {
    expect(certificateStatus({ ...cert, revoked: true }, 999)).toBe('REVOKED');
    expect(certificateStatus({ ...cert, revoked: true }, 2_000)).toBe('REVOKED');
  });
});
