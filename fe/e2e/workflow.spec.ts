import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { resolve } from 'node:path';
import { tmpdir } from 'node:os';
import { spawn } from 'node:child_process';

const password = readFileSync('../.local/e2e/demo.env', 'utf8')
  .split('\n')
  .find((line) => line.startsWith('DEMO_PASSWORD='))!
  .slice('DEMO_PASSWORD='.length);
const base = 'http://127.0.0.1:8082/api/v1';
async function signIn(page: Page, org = 'alpha', role = 'org_admin') {
  await page.goto('/sign-in');
  await page.getByLabel('Tổ chức', { exact: false }).fill(org);
  await page.getByLabel('Email', { exact: false }).fill(`${role}@demo.local`);
  await page.getByLabel('Mật khẩu', { exact: false }).fill(password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Tổng quan thiết bị' })).toBeVisible();
}
/** Sign out through the avatar menu in the top bar. */
async function signOut(page: Page) {
  await page.getByRole('button', { name: 'Tài khoản' }).click();
  await page.getByRole('menuitem', { name: 'Đăng xuất' }).click();
}

async function admin(request: APIRequestContext, org = 'alpha') {
  const csrf = await (await request.get(`${base}/auth/csrf`)).json();
  const response = await request.post(`${base}/auth/login`, {
    headers: { [csrf.headerName]: csrf.token },
    data: { organizationSlug: org, email: 'org_admin@demo.local', password },
  });
  expect(response.ok()).toBeTruthy();
  const { accessToken } = await response.json();
  return { Authorization: `Bearer ${accessToken}` };
}
async function simulator(dir: string, args: string[], token?: string, expectedCode = 0) {
  const code = await new Promise<number>((done, reject) => {
    const child = spawn(
      process.execPath,
      [
        '--import',
        'tsx',
        'src/cli.ts',
        ...args,
        '--base',
        'http://127.0.0.1:8082',
        '--state',
        dir,
        '--trust',
        '../.local/e2e/command-public.json',
      ],
      {
        cwd: resolve('../simulator'),
        env: { ...process.env, ...(token ? { ENROLLMENT_TOKEN: token } : {}) },
        stdio: ['ignore', 'ignore', 'pipe'],
      }
    );
    let errors = '';
    child.stderr.on('data', (chunk) => {
      errors += chunk.toString();
    });
    child.on('error', reject);
    child.on('exit', (value) =>
      value === expectedCode ? done(value) : reject(new Error(`Simulator exit ${value}: ${errors}`))
    );
  });
  expect(code).toBe(expectedCode);
}
test('login → simulator enroll → policy → signed command → audit → refresh/logout', async ({
  page,
}) => {
  const directory = await mkdtemp(resolve(tmpdir(), 'mdm-e2e-'));
  const name = `Interview Android ${Date.now()}`;
  try {
    await signIn(page);
    await page
      .getByRole('navigation')
      .getByRole('link', { name: 'Enrollment', exact: true })
      .click();
    await page.getByRole('button', { name: 'Tạo enrollment token' }).click();
    const token = await page.getByLabel('Enrollment token', { exact: true }).inputValue();
    await simulator(directory, ['enroll', '--uid', name, '--name', name], token);
    await simulator(directory, ['run', '--once']);
    await page.getByRole('navigation').getByRole('link', { name: 'Policies', exact: true }).click();
    await page.getByRole('button', { name: 'Tạo policy', exact: true }).click();
    await page.getByLabel('Tên policy', { exact: false }).fill(name);
    await page.getByRole('button', { name: 'Lưu version' }).click();
    await expect(page.getByRole('dialog')).not.toBeVisible();
    await page.getByRole('navigation').getByRole('link', { name: 'Thiết bị', exact: true }).click();
    await page.getByRole('link', { name, exact: true }).click();
    await page.getByLabel('Policy', { exact: true }).click();
    await page.getByRole('option', { name, exact: true }).click();
    await page.getByLabel('Version cần gán').click();
    await page.getByRole('option', { name: 'Version 1', exact: true }).click();
    await page.getByRole('button', { name: 'Gán version' }).click();
    await expect(page.getByText('Đã cập nhật assignment.', { exact: false })).toBeVisible();
    await simulator(directory, ['run', '--once']);
    await expect(page.getByText('Compliant', { exact: true })).toBeVisible();
    await page.getByLabel('Loại command').click();
    await page.getByRole('option', { name: 'SHOW_MESSAGE', exact: true }).click();
    await page.getByLabel('Nội dung thông báo', { exact: false }).fill('Interview demo');
    await page.getByRole('button', { name: 'Gửi command mô phỏng' }).click();
    await expect(page.getByText('Command đã được đưa vào hàng đợi.')).toBeVisible();
    await simulator(directory, ['run', '--once']);
    await expect(page.getByRole('cell', { name: 'Succeeded', exact: true })).toBeVisible();
    await page.getByRole('navigation').getByRole('link', { name: 'Audit', exact: true }).click();
    await expect(
      page.getByRole('cell', { name: 'COMMAND_SUCCEEDED', exact: true }).first()
    ).toBeVisible();
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Audit', exact: true })).toBeVisible();
    await signOut(page);
    await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});
test('lost result + restart deduplicates effect, isolates tenants and revokes credential', async ({
  request,
}) => {
  const directory = await mkdtemp(resolve(tmpdir(), 'mdm-recovery-'));
  try {
    const alpha = await admin(request);
    const token = await (
      await request.post(`${base}/enrollment-tokens`, { headers: alpha })
    ).json();
    await simulator(directory, ['enroll', '--uid', `restart-${Date.now()}`], token.token);
    const identity = JSON.parse(await readFile(resolve(directory, 'state.json'), 'utf8'));
    const created = await request.post(`${base}/devices/${identity.deviceId}/commands`, {
      headers: { ...alpha, 'Idempotency-Key': crypto.randomUUID() },
      data: { type: 'SHOW_MESSAGE', payload: { message: 'Only once' } },
    });
    expect(created.ok()).toBeTruthy();
    await simulator(directory, ['run', '--once', '--crash-after-result'], undefined, 75);
    await expect
      .poll(
        async () => {
          await simulator(directory, ['run', '--once']);
          const result = await (
            await request.get(`${base}/devices/${identity.deviceId}/commands`, { headers: alpha })
          ).json();
          return result.items[0].status;
        },
        { timeout: 15000, intervals: [1800] }
      )
      .toBe('SUCCEEDED');
    const state = JSON.parse(await readFile(resolve(directory, 'state.json'), 'utf8'));
    expect(state.messages).toHaveLength(1);
    const beta = await admin(request, 'beta');
    expect(
      (await request.get(`${base}/devices/${identity.deviceId}`, { headers: beta })).status()
    ).toBe(404);
    expect(
      (await request.post(`${base}/devices/${identity.deviceId}/revoke`, { headers: alpha })).ok()
    ).toBeTruthy();
    expect(
      (
        await request.post(`${base}/device-agent/heartbeat`, {
          headers: { Authorization: `Bearer ${identity.credential}` },
          data: state.inventory,
        })
      ).status()
    ).toBe(401);
  } finally {
    await rm(directory, { recursive: true, force: true });
  }
});
test('viewer cannot change resources and mobile navigation fits', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await signIn(page, 'alpha', 'viewer');
  await page.getByRole('button', { name: 'Menu', exact: true }).click();
  await page.getByRole('navigation').getByRole('link', { name: 'Policies', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Policies', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Tạo policy', exact: true })).toHaveCount(0);
  expect(
    await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)
  ).toBeTruthy();
});

test('two tabs serialize refresh and logout reaches both', async ({ page, context }) => {
  await signIn(page);
  const second = await context.newPage();
  await second.goto('/');
  await expect(second.getByRole('heading', { name: 'Tổng quan thiết bị' })).toBeVisible();
  await Promise.all([page.reload(), second.reload()]);
  await expect(page.getByRole('heading', { name: 'Tổng quan thiết bị' })).toBeVisible();
  await expect(second.getByRole('heading', { name: 'Tổng quan thiết bị' })).toBeVisible();
  await signOut(page);
  await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
  await expect(second.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
});
