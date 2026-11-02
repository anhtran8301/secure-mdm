import { generateKeyPairSync, randomBytes } from 'node:crypto';
import { mkdirSync, existsSync, writeFileSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(process.argv[2] || '.local');
mkdirSync(root, { recursive: true, mode: 0o700 });
const publicKeys = [];
for (const name of ['access', 'command-1', 'command-2']) {
  const filename = resolve(root, `${name}.json`);
  let jwk;
  if (existsSync(filename)) jwk = JSON.parse(readFileSync(filename, 'utf8'));
  else {
    const { privateKey } = generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
    jwk = { ...privateKey.export({ format: 'jwk' }), kid: name, alg: 'ES256', use: 'sig' };
    writeFileSync(filename, JSON.stringify(jwk), { mode: 0o600, flag: 'wx' });
  }
  if (name.startsWith('command')) {
    const { d, ...pub } = jwk;
    publicKeys.push(pub);
  }
}
writeFileSync(resolve(root, 'command-public.json'), JSON.stringify({ keys: publicKeys }), {
  mode: 0o600,
});
if (!existsSync(resolve(root, 'demo.env'))) {
  writeFileSync(
    resolve(root, 'demo.env'),
    `DEMO_PASSWORD=${randomBytes(18).toString('base64url')}\nDB_PASSWORD=${randomBytes(18).toString('base64url')}\nMYSQL_ROOT_PASSWORD=${randomBytes(18).toString('base64url')}\n`,
    { mode: 0o600, flag: 'wx' }
  );
}
console.log(
  `Keys and demo configuration ready in ${root}. Existing private keys/passwords were preserved.`
);
