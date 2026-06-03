import { defineConfig, devices } from '@playwright/test';
import { resolve } from 'node:path';
import { resolveE2EApiBaseURL } from '../../playwright.workspace';

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://a.mango.io:5176';
const uiRoot = resolve(__dirname, '../..');
const apiBaseURL = resolveE2EApiBaseURL({ uiRoot, defaultURL: 'http://127.0.0.1:5555' });
const frontendURL = new URL(baseURL);
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
            VITE_PORT: frontendURL.port,
          },
          reuseExistingServer,
          timeout: 120 * 1000,
        },
      }),
});
