import { createLocalJWKSet, jwtVerify, type JSONWebKeySet, type JWK } from 'jose';

/** The command keys this device trusts, and the issue time of the bundle they came from. */
export type TrustStore = JSONWebKeySet & { iat?: number };

const MAX_SKEW_S = 60;
const PUBLIC_FIELDS = ['kty', 'crv', 'x', 'y', 'kid', 'alg', 'use'] as const;

/**
 * Picks a Trust Bundle signed by a key this device already trusts (ADR 0006) and returns the new
 * store, or null when none verifies so the caller keeps its current trust.
 */
export async function refreshTrust(
  current: TrustStore,
  bundles: string[],
  now = new Date()
): Promise<TrustStore | null> {
  for (const bundle of bundles) {
    try {
      const { payload } = await jwtVerify(bundle, createLocalJWKSet(current), {
        algorithms: ['ES256'],
        typ: 'mdm-trust+jwt',
        issuer: 'secure-mdm',
        currentDate: now,
        requiredClaims: ['iat', 'exp', 'keys'],
      });
      const keys = payload.keys as JWK[];
      // A rollback to an older bundle could bring back a retired key; a future iat would make
      // every later bundle look like a rollback.
      if (current.iat !== undefined && payload.iat! < current.iat) continue;
      if (payload.iat! > now.getTime() / 1000 + MAX_SKEW_S) continue;
      if (!Array.isArray(keys) || !keys.length || !keys.every(isPublicCommandKey)) continue;
      return {
        keys: keys.map((key) =>
          Object.fromEntries(PUBLIC_FIELDS.filter((f) => f in key).map((f) => [f, key[f]]))
        ),
        iat: payload.iat,
      };
    } catch {
      // Malformed or unverifiable bundle: try the next one.
    }
  }
  return null;
}

function isPublicCommandKey(key: JWK) {
  return (
    key?.kty === 'EC' &&
    key.crv === 'P-256' &&
    typeof key.kid === 'string' &&
    typeof key.x === 'string' &&
    typeof key.y === 'string' &&
    !('d' in key)
  );
}
