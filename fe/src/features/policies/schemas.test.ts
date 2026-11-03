import { it, expect, describe } from 'vitest';

import { toPolicyConfig, policyFormSchema, policyFormDefaults } from './schemas';

describe('policy form', () => {
  it('requires disk encryption by default for a new policy', () => {
    expect(policyFormDefaults(null).requireDiskEncryption).toBe(true);
  });

  it('sends disk encryption to the API', () => {
    const values = policyFormSchema(true).parse({
      ...policyFormDefaults(null),
      name: 'Corporate',
      requireDiskEncryption: false,
      blockedPackages: 'com.bad\n',
    });
    expect(toPolicyConfig(values)).toEqual({
      requireScreenLock: true,
      requireDiskEncryption: false,
      minAndroidApiLevel: 34,
      blockedPackages: ['com.bad'],
    });
  });
});
