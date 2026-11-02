import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  timeout: 60000,
  expect: { timeout: 12000 },
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: { baseURL: 'http://127.0.0.1:3040', trace: 'retain-on-failure' },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: 'node ../scripts/backend.mjs --e2e',
      url: 'http://127.0.0.1:8082/actuator/health',
      timeout: 120000,
      reuseExistingServer: false,
    },
    {
      command: 'corepack yarn dev --host 127.0.0.1 --port 3040 --strictPort',
      env: { API_URL: 'http://127.0.0.1:8082' },
      url: 'http://127.0.0.1:3040',
      reuseExistingServer: false,
    },
  ],
});
