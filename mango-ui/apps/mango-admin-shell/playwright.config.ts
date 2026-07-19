import { defineConfig, devices } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { copyFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { resolveE2EApiBaseURL } from '../../playwright.workspace';

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://a.mango.io:5176';
const uiRoot = resolve(__dirname, '../..');
const apiBaseURL = resolveE2EApiBaseURL({ uiRoot, defaultURL: 'http://127.0.0.1:5555' });
const frontendURL = new URL(baseURL);
const useExternalWebServer = process.env.PLAYWRIGHT_USE_EXTERNAL_WEBSERVER === 'true';
const reuseExistingServer = process.env.PLAYWRIGHT_REUSE_EXISTING_SERVER === 'true';
const runtimeConfigPath =
  process.env.PLAYWRIGHT_RUNTIME_CONFIG_PATH ||
  resolve(uiRoot, '../.runtime/playwright/mango-admin-shell/runtime-config.json');
const reportPath =
  process.env.PLAYWRIGHT_JSON_REPORT_PATH || resolve(uiRoot, '../.runtime/playwright/mango-admin-shell/report.json');
const gitCommit = execFileSync('git', ['rev-parse', 'HEAD'], {
  cwd: uiRoot,
  encoding: 'utf8',
}).trim();
const gitTree = execFileSync('git', ['rev-parse', 'HEAD^{tree}'], {
  cwd: uiRoot,
  encoding: 'utf8',
}).trim();
mkdirSync(dirname(runtimeConfigPath), { recursive: true });
mkdirSync(dirname(reportPath), { recursive: true });
copyFileSync(resolve(__dirname, './runtime-config.dev.json'), runtimeConfigPath);
process.env.PLAYWRIGHT_RUNTIME_CONFIG_PATH = runtimeConfigPath;

export default defineConfig({
  testDir: './e2e',
  timeout: 120 * 1000,
  expect: {
    timeout: 8000,
  },
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: 1,
  metadata: {
    gitCommit,
    gitTree,
  },
  reporter: [['list'], ['json', { outputFile: reportPath }]],
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
          cwd: uiRoot,
          env: {
            VITE_ADMIN_PROXY_PATH: apiBaseURL,
            VITE_PORT: frontendURL.port,
            VITE_MANGO_RUNTIME_CONFIG_FILE: runtimeConfigPath,
          },
          reuseExistingServer,
          timeout: 120 * 1000,
        },
      }),
});
