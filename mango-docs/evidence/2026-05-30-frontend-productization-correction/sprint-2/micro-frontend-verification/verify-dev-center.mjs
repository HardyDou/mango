import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../../../../..');
const require = createRequire(path.join(repoRoot, 'mango-ui/apps/mango-admin-shell/package.json'));
const { chromium } = require('@playwright/test');

const baseUrl = process.env.MANGO_MICRO_FRONTEND_URL || 'http://a.mango.io:5176';
const screenshotDir = path.join(__dirname, 'screenshots');
const reportPath = path.join(__dirname, 'dev-center-report.json');

const pages = [
  {
    name: '文件上传',
    path: '/components/upload',
    expectedText: 'MUpload 文件上传',
    screenshot: 'micro-dev-center-upload-1440x960.png',
  },
  {
    name: '验证码',
    path: '/components/captcha',
    expectedText: '验证码组件',
    screenshot: 'micro-dev-center-captcha-1440x960.png',
  },
  {
    name: '实时通信',
    path: '/components/realtime',
    expectedText: 'RealtimeClient 实时通信组件',
    screenshot: 'micro-dev-center-realtime-1440x960.png',
  },
];

const state = {
  baseUrl,
  startedAt: new Date().toISOString(),
  screenshots: [],
  pageErrors: [],
  consoleErrors: [],
  networkFailures: [],
  checks: [],
};

async function screenshot(page, fileName) {
  const fullPath = path.join(screenshotDir, fileName);
  await page.screenshot({ path: fullPath, fullPage: false });
  state.screenshots.push(fullPath);
  return fullPath;
}

async function login(page) {
  await page.goto(`${baseUrl}/#/login`, { waitUntil: 'networkidle' });
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  const tenantResponse = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.locator('input[placeholder="密码"]').blur();
  await tenantResponse;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 15000 });
  await page.waitForLoadState('networkidle');
  await page.locator('.el-message').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => undefined);
  await page.waitForTimeout(800);
}

async function main() {
  await fs.mkdir(screenshotDir, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });

  page.on('console', (message) => {
    if (message.type() === 'error') {
      state.consoleErrors.push(message.text());
    }
  });
  page.on('pageerror', (error) => {
    state.pageErrors.push(error.message);
  });
  page.on('requestfailed', (request) => {
    state.networkFailures.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`);
  });

  await login(page);

  for (const target of pages) {
    await page.goto(`${baseUrl}/#${target.path}`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1200);
    const screenshotPath = await screenshot(page, target.screenshot);
    const bodyText = await page.locator('body').first().innerText();
    const check = {
      name: target.name,
      url: page.url(),
      screenshot: screenshotPath,
      has404: await page.getByText('404').isVisible().catch(() => false),
      hasExpectedText: await page.getByText(target.expectedText).first().isVisible().catch(() => false),
      hasShellTopbar: await page.locator('.layout-navbars-container').isVisible().catch(() => false),
      hasShellAside: await page.locator('.layout-aside').isVisible().catch(() => false),
      hasTagsView: await page.locator('.tags-view-container').isVisible().catch(() => false),
      hasNoticeBell: await page.locator('.notice-bell').isVisible().catch(() => false),
      bodyText: bodyText.slice(0, 2000),
    };
    state.checks.push(check);
  }

  await browser.close();
  state.finishedAt = new Date().toISOString();
  await fs.writeFile(reportPath, `${JSON.stringify(state, null, 2)}\n`, 'utf8');

  const failedChecks = state.checks.filter(check =>
    check.has404 || !check.hasExpectedText || !check.hasShellTopbar || !check.hasShellAside || !check.hasTagsView
  );
  console.log(JSON.stringify({
    reportPath,
    screenshots: state.screenshots,
    pageErrors: state.pageErrors,
    consoleErrors: state.consoleErrors,
    networkFailures: state.networkFailures,
    checks: state.checks.map(({ bodyText, ...check }) => check),
  }, null, 2));
  if (state.pageErrors.length > 0 || state.consoleErrors.length > 0 || failedChecks.length > 0) {
    process.exit(1);
  }
}

main().catch(async (error) => {
  state.failedAt = new Date().toISOString();
  state.error = error instanceof Error ? error.stack || error.message : String(error);
  await fs.writeFile(reportPath, `${JSON.stringify(state, null, 2)}\n`, 'utf8').catch(() => undefined);
  console.error(error);
  process.exit(1);
});
