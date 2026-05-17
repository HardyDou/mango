import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://a.mango.io:5176';
const useExternalWebServer = process.env.PLAYWRIGHT_USE_EXTERNAL_WEBSERVER !== 'false';

export default defineConfig({
  testDir: './e2e',
  timeout: 45 * 1000,
  expect: {
    timeout: 8000,
  },
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  reporter: 'list',
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], channel: 'chrome' },
    },
  ],
  ...(useExternalWebServer
    ? {}
    : {
        webServer: {
          command: 'pnpm run dev',
          url: baseURL,
          reuseExistingServer: true,
          timeout: 120 * 1000,
        },
      }),
});
