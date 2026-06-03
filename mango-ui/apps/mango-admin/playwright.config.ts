import { defineConfig, devices } from '@playwright/test';
import { resolve } from 'node:path';
import { resolveE2EApiBaseURL, resolveE2EBaseURL } from '../../playwright.workspace';

const uiRoot = resolve(__dirname, '../..');
const baseURL = resolveE2EBaseURL({ uiRoot, defaultURL: 'http://127.0.0.1:7777' });
const apiBaseURL = resolveE2EApiBaseURL({ uiRoot, defaultURL: 'http://127.0.0.1:5555' });
const frontendURL = new URL(baseURL);
const useExternalWebServer = process.env.PLAYWRIGHT_USE_EXTERNAL_WEBSERVER === 'true';

export default defineConfig({
  testDir: './e2e',
  timeout: 30 * 1000,
  expect: {
    timeout: 5000,
  },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
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
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
  ],
  ...(useExternalWebServer
    ? {}
    : {
        webServer: {
          command: 'pnpm run dev',
          url: baseURL,
          env: {
            VITE_ADMIN_PROXY_PATH: apiBaseURL,
            VITE_PORT: frontendURL.port,
          },
          reuseExistingServer: !process.env.CI,
          timeout: 120 * 1000,
        },
      }),
});
