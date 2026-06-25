import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5191';
const frontendURL = new URL(baseURL);
const useExternalWebServer = process.env.PLAYWRIGHT_USE_EXTERNAL_WEBSERVER === 'true';

export default defineConfig({
  testDir: './e2e/specs',
  timeout: 30 * 1000,
  expect: { timeout: 5000 },
  fullyParallel: false,
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
          command: `pnpm exec vite --host ${frontendURL.hostname} --port ${frontendURL.port}`,
          url: baseURL,
          reuseExistingServer: true,
          timeout: 120 * 1000,
        },
      }),
});
