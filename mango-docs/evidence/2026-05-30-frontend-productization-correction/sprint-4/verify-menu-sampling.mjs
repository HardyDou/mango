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
const screenshotDir = path.join(__dirname, 'screenshots');
const reportPath = path.join(__dirname, 'menu-sampling-report.json');
const seed = process.env.MANGO_MENU_SAMPLE_SEED || 'sprint-4-menu-sampling';

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
  seed,
  startedAt: new Date().toISOString(),
  screenshots: [],
  pageErrors: [],
  consoleErrors: [],
  networkFailures: [],
  ignoredNetworkFailures: [],
  failedResponses: [],
};

async function screenshot(page, fileName, options = {}) {
  const fullPath = path.join(screenshotDir, fileName);
  await page.screenshot({ path: fullPath, fullPage: options.fullPage ?? false });
  state.screenshots.push(fullPath);
  return fullPath;
}

function sanitizeFileName(value) {
  return value.replace(/[\\/:*?"<>|\s]+/g, '-');
}

function flattenRunnableMenus(menus, topMenu, parentNames = []) {
  const rows = [];
  for (const menu of menus || []) {
    const names = [...parentNames, menu.menuName].filter(Boolean);
    if (isRunnablePage(menu)) {
      rows.push({
        topLevelMenu: topMenu.menuName,
        menuId: menu.menuId,
        menuName: menu.menuName,
        fullName: names.join(' / '),
        menuCode: menu.menuCode,
        moduleCode: menu.moduleCode,
        path: menu.path,
        component: menu.component,
        pageType: menu.pageType,
        permissions: menu.permissions,
      });
    }
    rows.push(...flattenRunnableMenus(menu.children || [], topMenu, names));
  }
  return rows;
}

function isRunnablePage(menu) {
  return menu
    && menu.menuType !== 3
    && menu.visible !== 0
    && menu.status !== 0
    && typeof menu.path === 'string'
    && menu.path.startsWith('/')
    && (menu.component || menu.pageType === 'IFRAME' || menu.pageType === 'EXTERNAL_LINK');
}

function seededSample(rows, count) {
  return [...rows]
    .sort((left, right) => hash(`${seed}:${left.menuId}:${left.path}`) - hash(`${seed}:${right.menuId}:${right.path}`))
    .slice(0, count);
}

function hash(value) {
  let result = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    result ^= value.charCodeAt(index);
    result = Math.imul(result, 16777619);
  }
  return result >>> 0;
}

function isIgnoredNetworkFailure(request) {
  const failureText = request.failure()?.errorText || '';
  return request.url().includes('/api/realtime/transports/probe/sse')
    && failureText.includes('ERR_ABORTED');
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
  if (!menuResponse.ok() || !Array.isArray(menuBody?.data)) {
    throw new Error(`Menu API failed: ${menuResponse.status()} ${JSON.stringify(menuBody)}`);
  }
  return {
    api,
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
    backendMenus: menuBody.data,
  };
}

async function loginUi(page) {
  await page.goto(`${frontendUrl}/#/login`, { waitUntil: 'networkidle' });
  await page.waitForSelector('.login-container', { timeout: 15000 });
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
    response.url().includes('/api/authorization/menus/user')
    && response.url().includes('fmt=tree')
    && response.status() === 200
  );
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 15000 });
  await menuResponse;
  await page.waitForLoadState('networkidle');
  await page.locator('.el-message').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => undefined);
  await page.waitForTimeout(800);
}

async function collectUiTopMenus(page) {
  return page.locator('.layout-top-system-item').evaluateAll((nodes) => nodes
    .map(node => node.textContent?.trim().replace(/\s+/g, ''))
    .filter(Boolean));
}

async function collectPageEvidence(page, sample) {
  const errorText = page.locator('text=/401|403|404|未授权|拒绝访问|路由加载失败|页面加载失败|加载失败|暂无可访问菜单/').first();
  const runtimeData = await page.locator('.shell-runtime-content').evaluate((el) => ({
    moduleCode: el.dataset.mangoRuntimeModule || '',
    runtimeCode: el.dataset.mangoRuntimeCode || '',
    pageType: el.dataset.mangoRuntimePageType || '',
    entry: el.dataset.mangoRuntimeEntry || '',
  })).catch(() => ({}));
  return {
    url: page.url(),
    titleVisible: await page.getByText(sample.menuName).first().isVisible().catch(() => false),
    fullNameTailVisible: await page.getByText(sample.fullName.split(' / ').at(-1) || sample.menuName).first().isVisible().catch(() => false),
    has404OrError: await errorText.isVisible().catch(() => false),
    shell: {
      topbarVisible: await page.locator('.layout-navbars-container').first().isVisible().catch(() => false),
      asideVisible: await page.locator('.layout-aside').first().isVisible().catch(() => false),
      tagsVisible: await page.locator('.tags-view-container').first().isVisible().catch(() => false),
      noticeBellVisible: await page.locator('.notice-bell').first().isVisible().catch(() => false),
      settingsVisible: await page.locator('.layout-breadcrumb-settings').first().isVisible().catch(() => false),
      userVisible: await page.locator('.layout-breadcrumb-user').first().isVisible().catch(() => false),
      activeTopMenus: await collectUiTopMenus(page),
    },
    runtime: runtimeData,
    bodyTextSample: (await page.locator('main').innerText().catch(() => '')).slice(0, 500),
  };
}

function createSamplingPlan(backendMenus) {
  return backendMenus
    .filter(menu => menu.menuType !== 3 && menu.visible !== 0 && menu.status !== 0)
    .map((topMenu) => {
      const runnablePages = flattenRunnableMenus(topMenu.children || [], topMenu);
      return {
        topLevelMenu: topMenu.menuName,
        path: topMenu.path,
        runnablePageCount: runnablePages.length,
        samples: seededSample(runnablePages, Math.min(3, Math.max(1, runnablePages.length))),
      };
    });
}

async function main() {
  await fs.mkdir(screenshotDir, { recursive: true });
  const apiState = await loginApi();
  const samplingPlan = createSamplingPlan(apiState.backendMenus);
  const emptyTopMenus = samplingPlan.filter(item => item.samples.length === 0);
  if (emptyTopMenus.length > 0) {
    throw new Error(`No runnable child page found under top menus: ${emptyTopMenus.map(item => item.topLevelMenu).join(', ')}`);
  }

  const browser = await chromium.launch({
    headless: true,
    args: [
      '--host-resolver-rules=MAP d.mango.io 127.0.0.1',
    ],
  });
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });

  page.on('console', (message) => {
    if (message.type() === 'error') {
      state.consoleErrors.push(message.text());
    }
  });
  page.on('pageerror', (error) => {
    state.pageErrors.push(error.stack || error.message);
  });
  page.on('requestfailed', (request) => {
    if (isIgnoredNetworkFailure(request)) {
      state.ignoredNetworkFailures.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`);
      return;
    }
    state.networkFailures.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`);
  });
  page.on('response', (response) => {
    if (response.status() >= 500) {
      state.failedResponses.push(`${response.status()} ${response.url()}`);
    }
  });

  await loginUi(page);
  await screenshot(page, 'menu-sampling-home-1440x960.png');
  const uiTopMenus = await collectUiTopMenus(page);
  const results = [];
  for (const topMenu of samplingPlan) {
    const sampledChildren = [];
    for (const sample of topMenu.samples) {
      await page.goto(`${frontendUrl}/#${sample.path}`, { waitUntil: 'domcontentloaded' });
      await page.waitForURL(`**/#${sample.path}`, { timeout: 15000 });
      await page.locator('.shell-runtime-content').waitFor({ state: 'visible', timeout: 15000 });
      await page.waitForTimeout(1200);
      const fileName = `s4-sample-${sanitizeFileName(topMenu.topLevelMenu)}-${sanitizeFileName(sample.menuName)}.png`;
      const screenshotPath = await screenshot(page, fileName, { fullPage: true });
      sampledChildren.push({
        ...sample,
        screenshot: screenshotPath,
        evidence: await collectPageEvidence(page, sample),
      });
    }
    results.push({
      topLevelMenu: topMenu.topLevelMenu,
      path: topMenu.path,
      runnablePageCount: topMenu.runnablePageCount,
      sampledChildren,
    });
  }

  await browser.close();
  await apiState.api.dispose();

  state.finishedAt = new Date().toISOString();
  const report = {
    ...state,
    api: {
      health: apiState.health,
      login: apiState.login,
      backendTopMenus: apiState.backendMenus.map(menu => menu.menuName),
    },
    uiTopMenus,
    sampling: results,
  };
  await fs.writeFile(reportPath, `${JSON.stringify(report, null, 2)}\n`, 'utf8');
  console.log(JSON.stringify({
    reportPath,
    screenshots: state.screenshots,
    sampledTopMenus: results.map(item => ({
      topLevelMenu: item.topLevelMenu,
      sampled: item.sampledChildren.map(child => child.menuName),
    })),
    pageErrors: state.pageErrors,
    consoleErrors: state.consoleErrors,
    networkFailures: state.networkFailures,
    ignoredNetworkFailures: state.ignoredNetworkFailures,
    failedResponses: state.failedResponses,
  }, null, 2));

  const failedSamples = results.flatMap(item => item.sampledChildren.filter(child =>
    child.evidence.has404OrError
    || !child.evidence.shell.topbarVisible
    || !child.evidence.shell.asideVisible
    || !child.evidence.shell.tagsVisible
  ));
  if (
    state.pageErrors.length > 0
    || state.consoleErrors.length > 0
    || state.networkFailures.length > 0
    || state.failedResponses.length > 0
    || failedSamples.length > 0
  ) {
    throw new Error(`Menu sampling verification failed. failedSamples=${failedSamples.map(item => item.fullName).join(', ')}`);
  }
}

main().catch(async (error) => {
  state.failedAt = new Date().toISOString();
  state.error = error instanceof Error ? error.stack || error.message : String(error);
  await fs.writeFile(reportPath, `${JSON.stringify(state, null, 2)}\n`, 'utf8').catch(() => undefined);
  console.error(error);
  process.exit(1);
});
