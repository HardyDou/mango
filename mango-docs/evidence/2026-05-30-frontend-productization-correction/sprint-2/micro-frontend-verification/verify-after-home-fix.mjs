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
const reportPath = path.join(__dirname, 'after-home-fix-report.json');

const state = {
  baseUrl,
  startedAt: new Date().toISOString(),
  screenshots: [],
  menuResponses: [],
  pageErrors: [],
  consoleErrors: [],
  networkFailures: [],
  checks: {},
};

async function screenshot(page, fileName) {
  const fullPath = path.join(screenshotDir, fileName);
  await page.screenshot({ path: fullPath, fullPage: false });
  state.screenshots.push(fullPath);
}

async function collectTexts(page, selector) {
  return page.locator(selector).evaluateAll((nodes) => nodes
    .map((node) => node.textContent?.trim().replace(/\s+/g, ' '))
    .filter(Boolean));
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
  page.on('response', async (response) => {
    if (!response.url().includes('/api/authorization/menus/user')) {
      return;
    }
    let body = '';
    try {
      body = await response.text();
    } catch {
      body = '<unreadable>';
    }
    state.menuResponses.push({
      url: response.url(),
      status: response.status(),
      body: body.slice(0, 3000),
    });
  });

  await login(page);
  await screenshot(page, 'micro-home-after-home-fix-1440x960.png');
  state.checks.home = {
    url: page.url(),
    bodyText: await page.locator('body').first().innerText(),
    topMenus: await collectTexts(page, '.layout-top-system-item'),
    sideMenus: await collectTexts(page, '.el-menu .el-menu-item, .el-menu .el-sub-menu__title'),
    has404: await page.getByText('404').isVisible().catch(() => false),
    hasHomeContent: await page.getByText('数据概览').isVisible().catch(() => false),
    hasNoticeBell: await page.locator('.notice-bell').isVisible().catch(() => false),
    hasSettings: await page.locator('.layout-breadcrumb-settings').isVisible().catch(() => false),
    hasUser: await page.locator('.layout-breadcrumb-user').isVisible().catch(() => false),
  };

  await page.goto(`${baseUrl}/#/system/menu-package`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  await screenshot(page, 'micro-rbac-menu-package-after-home-fix-1440x960.png');
  state.checks.menuPackage = {
    url: page.url(),
    bodyText: await page.locator('body').first().innerText(),
    has404: await page.getByText('404').isVisible().catch(() => false),
    hasMenuPackageText: await page.getByText('套餐管理').first().isVisible().catch(() => false),
    hasCreatePackageText: await page.getByText('新增套餐').first().isVisible().catch(() => false),
  };

  await browser.close();
  state.finishedAt = new Date().toISOString();
  await fs.writeFile(reportPath, `${JSON.stringify(state, null, 2)}\n`, 'utf8');
  console.log(JSON.stringify({
    reportPath,
    screenshots: state.screenshots,
    pageErrors: state.pageErrors,
    consoleErrors: state.consoleErrors,
    networkFailures: state.networkFailures,
    checks: {
      home: {
        url: state.checks.home.url,
        topMenus: state.checks.home.topMenus,
        sideMenus: state.checks.home.sideMenus,
        has404: state.checks.home.has404,
        hasHomeContent: state.checks.home.hasHomeContent,
        hasNoticeBell: state.checks.home.hasNoticeBell,
        hasSettings: state.checks.home.hasSettings,
        hasUser: state.checks.home.hasUser,
      },
      menuPackage: {
        url: state.checks.menuPackage.url,
        has404: state.checks.menuPackage.has404,
        hasMenuPackageText: state.checks.menuPackage.hasMenuPackageText,
        hasCreatePackageText: state.checks.menuPackage.hasCreatePackageText,
      },
    },
  }, null, 2));
}

main().catch(async (error) => {
  state.failedAt = new Date().toISOString();
  state.error = error instanceof Error ? error.stack || error.message : String(error);
  await fs.writeFile(reportPath, `${JSON.stringify(state, null, 2)}\n`, 'utf8').catch(() => undefined);
  console.error(error);
  process.exit(1);
});
