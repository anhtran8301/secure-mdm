import { parseArgs } from 'node:util';
import { setTimeout } from 'node:timers/promises';
import { resolve } from 'node:path';
import { withLock, load } from './state.js';
import { api, enroll, tick, ApiError, InterruptedAfterResult } from './agent.js';

const { values, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    base: { type: 'string', default: 'http://127.0.0.1:8080' },
    state: { type: 'string', default: '../.local/device' },
    trust: { type: 'string', default: '../.local/command-public.json' },
    uid: { type: 'string', default: 'demo-device' },
    name: { type: 'string', default: 'Demo Android simulator' },
    once: { type: 'boolean' },
    'crash-after-result': { type: 'boolean' },
    fail: { type: 'boolean' },
    'offline-ms': { type: 'string', default: '0' },
  },
});
const directory = resolve(values.state!);
async function main() {
  api(values.base!); // validate transport even for enrollment
  await withLock(directory, async () => {
    switch (positionals[0]) {
      case 'enroll': {
        const token = process.env.ENROLLMENT_TOKEN;
        if (!token) throw new Error('Set ENROLLMENT_TOKEN in the environment');
        console.log(
          `Enrolled device ${await enroll(values.base!, directory, token, values.uid!, values.name!)}`
        );
        break;
      }
      case 'status': {
        const state = await load(directory);
        console.log(
          JSON.stringify(
            {
              deviceId: state.deviceId,
              organizationId: state.organizationId,
              expiresAt: state.expiresAt,
              inventory: state.inventory,
              policyRevision: state.policyRevision,
              completedCommands: Object.keys(state.results).length,
              messages: state.messages,
            },
            null,
            2
          )
        );
        break;
      }
      case 'run': {
        const offline = Number(values['offline-ms']);
        if (!Number.isFinite(offline) || offline < 0 || offline > 3600000)
          throw new Error('offline-ms must be 0..3600000');
        await setTimeout(offline);
        let stop = false,
          lastHeartbeat = 0,
          backoff = 1000;
        process.once('SIGINT', () => {
          stop = true;
        });
        process.once('SIGTERM', () => {
          stop = true;
        });
        do {
          try {
            const heartbeat = Date.now() - lastHeartbeat >= 15000;
            await tick(values.base!, directory, resolve(values.trust!), {
              heartbeat,
              crash: values['crash-after-result'],
              fail: values.fail,
            });
            if (heartbeat) lastHeartbeat = Date.now();
            backoff = 1000;
          } catch (error) {
            if (
              error instanceof InterruptedAfterResult ||
              (error instanceof ApiError && error.status === 401) ||
              values.once
            )
              throw error;
            console.error(
              error instanceof ApiError
                ? error.message
                : 'Sync failed; will retry (no credentials logged)'
            );
            await setTimeout(backoff);
            backoff = Math.min(backoff * 2, 30000);
          }
          if (!values.once && !stop) await setTimeout(3000);
        } while (!values.once && !stop);
        break;
      }
      default:
        throw new Error('Usage: enroll | run | status [--base URL] [--state DIR] [--trust JWKS]');
    }
  });
}
main().catch((error) => {
  console.error(error.message);
  process.exitCode = error instanceof InterruptedAfterResult ? 75 : 1;
});
