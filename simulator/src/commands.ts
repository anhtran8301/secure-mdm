import { createHash } from 'node:crypto';
import { createLocalJWKSet, jwtVerify, type JSONWebKeySet } from 'jose';
import type { State, Outcome } from './state.js';

export function verifier(jwks: JSONWebKeySet) {
  return createLocalJWKSet(jwks);
}
export async function execute(
  state: State,
  signed: string,
  keys: ReturnType<typeof verifier>,
  fail = false,
  now = new Date(),
  expectedId?: string
): Promise<{ state: State; outcome: Outcome; commandType: string }> {
  const { payload } = await jwtVerify(signed, keys, {
    algorithms: ['ES256'],
    typ: 'mdm-command+jwt',
    issuer: 'secure-mdm',
    audience: state.deviceId,
    currentDate: now,
    requiredClaims: ['exp', 'iat', 'jti', 'tenant', 'commandType', 'payload'],
  });
  if (
    payload.tenant !== state.organizationId ||
    typeof payload.jti !== 'string' ||
    (expectedId !== undefined && payload.jti !== expectedId) ||
    (payload.iat ?? 0) > now.getTime() / 1000 + 30
  )
    throw new Error('Command identity or issue time rejected');
  const type = payload.commandType;
  const args = payload.payload;
  if (!args || typeof args !== 'object' || Array.isArray(args))
    throw new Error('Invalid command payload');
  const fields = args as Record<string, unknown>;
  if (type === 'SHOW_MESSAGE') {
    if (
      Object.keys(fields).join() !== 'message' ||
      typeof fields.message !== 'string' ||
      !fields.message.trim() ||
      fields.message.length > 500
    )
      throw new Error('Invalid message');
  } else if (
    !['REPORT_STATUS', 'SYNC_DEVICE', 'LOCK_DEVICE_SIMULATED'].includes(String(type)) ||
    Object.keys(fields).length
  )
    throw new Error('Unsupported command');
  const digest = createHash('sha256')
    .update(JSON.stringify([payload.jti, type, fields]))
    .digest('hex');
  const cached = state.results[payload.jti];
  if (cached) {
    if (cached.digest !== digest) throw new Error('Command ID reused with different content');
    return { state, outcome: cached, commandType: String(type) };
  }
  const next = structuredClone(state);
  let result: Record<string, unknown>;
  if (fail) result = { errorCode: 'SIMULATED_FAILURE' };
  else
    switch (type) {
      case 'REPORT_STATUS':
        result = { inventory: structuredClone(next.inventory) };
        break;
      case 'SYNC_DEVICE':
        result = { synced: true };
        break;
      case 'SHOW_MESSAGE':
        next.messages.push({
          commandId: payload.jti,
          message: fields.message as string,
        });
        result = { displayed: true };
        break;
      case 'LOCK_DEVICE_SIMULATED':
        next.inventory.screenLockEnabled = true;
        result = { locked: true };
        break;
      default:
        throw new Error('Unsupported command');
    }
  const outcome: Outcome = {
    digest,
    status: fail ? 'FAILED' : 'SUCCEEDED',
    result,
  };
  next.results[payload.jti] = outcome;
  return { state: next, outcome, commandType: String(type) };
}
