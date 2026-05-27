import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://a.mango.io:5176';
const apiBaseURL = process.env.PLAYWRIGHT_API_BASE_URL || process.env.VITE_ADMIN_PROXY_PATH || 'http://127.0.0.1:5555';
const useExternalWebServer = process.env.PLAYWRIGHT_USE_EXTERNAL_WEBSERVER === 'true';
const reuseExistingServer = process.env.PLAYWRIGHT_REUSE_EXISTING_SERVER === 'true';

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
          command: 'pnpm dev:micro',
          url: baseURL,
          cwd: process.cwd(),
          env: {
            VITE_ADMIN_PROXY_PATH: apiBaseURL,
          },
          reuseExistingServer,
          timeout: 120 * 1000,
        },
      }),
});
