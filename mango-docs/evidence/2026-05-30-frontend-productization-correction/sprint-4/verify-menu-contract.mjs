import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../../../..');
const require = createRequire(path.join(repoRoot, 'mango-ui/apps/mango-admin-shell/package.json'));
const { chromium, request: playwrightRequest } = require('@playwright/test');

const frontendUrl = process.env.MANGO_MICRO_FRONTEND_URL || 'http://a.mango.io:5176';
const backendUrl = process.env.MANGO_BACKEND_URL || 'http://127.0.0.1:18800';
const allowDevCenter = process.env.MANGO_EXPECT_DEV_CENTER !== 'false';
const screenshotDir = path.join(__dirname, 'screenshots');
const reportPath = path.join(__dirname, 'menu-contract-report.json');

const credentials = {
  username: 'admin',
  password: 'admin123',
  tenantCode: 'default',
  realm: 'INTERNAL',
  actorType: 'INTERNAL_USER',
  partyType: 'INTERNAL_ORG',
  appCode: 'internal-admin',
};

const state = {
  frontendUrl,
  backendUrl,
  allowDevCenter,
  startedAt: new Date().toISOString(),
  screenshots: [],
  pageErrors: [],
  consoleErrors: [],
  networkFailures: [],
};

async function screenshot(page, fileName) {
  const fullPath = path.join(screenshotDir, fileName);
  await page.screenshot({ path: fullPath, fullPage: false });
  state.screenshots.push(fullPath);
  return fullPath;
}

async function loginApi() {
  const api = await playwrightRequest.newContext({ baseURL: backendUrl });
  const health = await api.get('/actuator/health');
  const loginResponse = await api.post('/auth/login', { data: credentials });
  const loginBody = await loginResponse.json();
  const token = loginBody?.data?.accessToken || loginBody?.data?.token;
  if (!loginResponse.ok() || !token) {
    throw new Error(`Backend login failed: ${loginResponse.status()} ${JSON.stringify(loginBody)}`);
  }
  const menuResponse = await api.get('/authorization/menus/user', {
    params: {
      fmt: 'tree',
      appCode: 'internal-admin',
    },
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Tenant-Id': String(loginBody?.data?.tenantId || credentials.tenantCode),
    },
  });
  const menuBody = await menuResponse.json();
  return {
    health: {
      status: health.status(),
      body: await health.json(),
    },
    login: {
      status: loginResponse.status(),
      success: loginBody?.success,
      hasAccessToken: Boolean(token),
      tenantName: loginBody?.data?.tenantName,
    },
    menu: {
      status: menuResponse.status(),
      success: menuBody?.success,
      topLevelMenus: (menuBody?.data || []).map(menu => menu.menuName),
      raw: menuBody,
    },
  };
}

async function loginUi(page) {
  await page.goto(`${frontendUrl}/#/login`, { waitUntil: 'networkidle' });
  await page.waitForSelector('.login-container', { timeout: 15000 });
  await screenshot(page, 's4-login-current-1440x960.png');
  await page.fill('input[placeholder="用户名"]', credentials.username);
  await page.fill('input[placeholder="密码"]', credentials.password);
  const tenantResponse = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  ).catch(() => undefined);
  await page.locator('input[placeholder="密码"]').blur();
  await Promise.race([
    tenantResponse,
    page.locator('.tenant-select', { hasText: '芒果集团' }).waitFor({ timeout: 5000 }).catch(() => undefined),
  ]);
  const menuResponse = page.waitForResponse((response) =>
    response.url().includes('/api/authorization/menus/user') && response.status() === 200
  );
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 15000 });
  await menuResponse;
  await page.waitForLoadState('networkidle');
  await page.locator('.el-message').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => undefined);
  await page.waitForTimeout(800);
}

async function collectTopMenus(page) {
  return page.locator('.layout-top-system-item').evaluateAll((nodes) => nodes
    .map(node => node.textContent?.trim().replace(/\s+/g, ''))
    .filter(Boolean));
}

function diffMenus(backendTopMenus, uiTopMenus) {
  const allowedShellMenus = ['首页', ...(allowDevCenter ? ['开发中心'] : [])];
  const allowedShellMenuReason = {
    首页: 'mango-shell built-in home route',
    开发中心: 'mango-shell configurable development center, dev/test default visible',
  };
  const unexpected = uiTopMenus.filter(name => !backendTopMenus.includes(name) && !allowedShellMenus.includes(name));
  const missing = backendTopMenus.filter(name => !uiTopMenus.includes(name));
  const shellMenus = uiTopMenus.filter(name => allowedShellMenus.includes(name));
  return { allowedShellMenus, allowedShellMenuReason, shellMenus, unexpected, missing };
}

async function main() {
  await fs.mkdir(screenshotDir, { recursive: true });
  const apiState = await loginApi();
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

  await loginUi(page);
  await screenshot(page, 'menu-contract-home-1440x960.png');
  const uiTopMenus = await collectTopMenus(page);
  const menuDiff = diffMenus(apiState.menu.topLevelMenus, uiTopMenus);
  const checks = {
    backendTopMenus: apiState.menu.topLevelMenus,
    uiTopMenus,
    ...menuDiff,
    hasNoticeBell: await page.locator('.notice-bell').isVisible().catch(() => false),
    hasSettings: await page.locator('.layout-breadcrumb-settings').isVisible().catch(() => false),
    hasUser: await page.locator('.layout-breadcrumb-user').isVisible().catch(() => false),
    hasLoginContainer: state.screenshots.some((item) => item.endsWith('s4-login-current-1440x960.png')),
    has404: await page.getByText('404').isVisible().catch(() => false),
  };

  await browser.close();
  state.finishedAt = new Date().toISOString();
  const report = {
    ...state,
    api: apiState,
    checks,
  };
  await fs.writeFile(reportPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8');
  console.log(JSON.stringify({
    reportPath,
    screenshots: state.screenshots,
    pageErrors: state.pageErrors,
    consoleErrors: state.consoleErrors,
    networkFailures: state.networkFailures,
    checks,
  }, null, 2));
  if (state.pageErrors.length > 0 || state.consoleErrors.length > 0 || checks.unexpected.length > 0 || checks.missing.length > 0 || checks.has404) {
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
