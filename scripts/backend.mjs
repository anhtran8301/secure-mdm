import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
import { spawn } from 'node:child_process';

const root = fileURLToPath(new URL('..', import.meta.url));
const e2e = process.argv.includes('--e2e');
const local = resolve(root, e2e ? '.local/e2e' : '.local');
const settings = Object.fromEntries(
  readFileSync(resolve(local, 'demo.env'), 'utf8')
    .trim()
    .split('\n')
    .map((line) => {
      const i = line.indexOf('=');
      return [line.slice(0, i), line.slice(i + 1)];
    })
);
const java = process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin/java') : 'java';
const inherited = { ...process.env };
// Generic Node/tool DEBUG variables must not enable Spring's request-body debug logging.
delete inherited.DEBUG;
delete inherited.TRACE;
const child = spawn(java, ['-jar', resolve(root, 'be/target/secure-mdm-0.0.1-SNAPSHOT.jar')], {
  cwd: resolve(root, 'be'),
  stdio: 'inherit',
  env: {
    ...inherited,
    ...settings,
    SPRING_PROFILES_ACTIVE: 'demo',
    DB_HOST: '127.0.0.1',
    DB_PORT: e2e ? '3308' : '3307',
    SERVER_PORT: e2e ? '8082' : '8080',
    ACCESS_KEY_FILE: resolve(local, 'access.json'),
    COMMAND_KEY_FILE: resolve(local, 'command-1.json'),
    SECURE_COOKIES: 'false',
    ...(e2e ? { MDM_LEASE_MS: '1500', MDM_SWEEP_MS: '1000' } : {}),
  },
});
for (const signal of ['SIGINT', 'SIGTERM']) process.once(signal, () => child.kill(signal));
child.on('exit', (code) => {
  process.exitCode = code || 0;
});
child.on('error', (error) => {
  console.error(error.message);
  process.exitCode = 1;
});
