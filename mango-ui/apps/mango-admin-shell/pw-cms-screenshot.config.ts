import { defineConfig, devices } from '@playwright/test';
export default defineConfig({
  testDir: './e2e',
  timeout: 120 * 1000,
  expect: { timeout: 8000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { outputFolder: 'pw-cms-report', open: 'never' }]],
  use: {
    baseURL: 'http://a.mango.io:5176',
    trace: 'retain-on-failure',
    screenshot: 'on',
    video: 'retain-on-failure',
    ...devices['Desktop Chrome'],
    channel: 'chrome',
  },
});
