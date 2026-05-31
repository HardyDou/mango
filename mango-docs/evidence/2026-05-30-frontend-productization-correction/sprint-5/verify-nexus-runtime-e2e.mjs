import { cpSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { execFileSync, spawn } from 'node:child_process';
import { tmpdir } from 'node:os';
import { createRequire } from 'node:module';

const repoRoot = process.cwd();
const require = createRequire(join(repoRoot, 'mango-ui/node_modules/.pnpm/node_modules/playwright/package.json'));
const { chromium, request: playwrightRequest } = require('playwright');

const evidenceDir = join(repoRoot, 'mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5');
const consumerTemplateDir = join(evidenceDir, 'consumer-app');
const screenshotDir = join(evidenceDir, 'screenshots');
const reportPath = join(evidenceDir, 'nexus-runtime-e2e-report.json');
const workspaceRoot = join(tmpdir(), `mango-sprint-5-nexus-runtime-${Date.now()}`);
const appDir = join(workspaceRoot, 'app');
const registry = process.env.MANGO_NPM_GROUP_REGISTRY || 'http://nexus.inner.yunxinbaokeji.com/repository/npm-group/';
const packageVersion = process.env.MANGO_ADMIN_PACKAGE_VERSION || '1.0.3';
const frontendPort = Number(process.env.MANGO_S5_CONSUMER_PORT || 18605);
const frontendUrl = `http://127.0.0.1:${frontendPort}`;
const backendUrl = process.env.MANGO_BACKEND_URL || 'http://127.0.0.1:18800';
const credentials = {
  username: process.env.MANGO_E2E_USERNAME || 'admin',
  password: process.env.MANGO_E2E_PASSWORD || 'admin123',
  tenantNamePattern: process.env.MANGO_E2E_TENANT_NAME || '芒果集团',
  tenantCode: process.env.MANGO_E2E_TENANT_CODE || 'default',
  realm: 'INTERNAL',
  actorType: 'INTERNAL_USER',
  partyType: 'INTERNAL_ORG',
  appCode: 'internal-admin',
};
const loginPayload = {
  username: credentials.username,
  password: credentials.password,
  tenantCode: credentials.tenantCode,
  realm: credentials.realm,
  actorType: credentials.actorType,
  partyType: credentials.partyType,
  appCode: credentials.appCode,
};

const commands = [];
const screenshots = [];
const checks = [];
const consoleErrors = [];
const pageErrors = [];
const networkFailures = [];
const badResponses = [];
const serverLogs = [];
let devServer;

mkdirSync(screenshotDir, { recursive: true });

function run(command, args, options = {}) {
  const startedAt = new Date().toISOString();
  try {
    const stdout = execFileSync(command, args, {
      cwd: options.cwd || repoRoot,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      env: { ...process.env, ...options.env },
    });
    commands.push({ command, args, cwd: options.cwd || repoRoot, ok: true, startedAt, stdout });
    return stdout;
  } catch (error) {
    commands.push({
      command,
      args,
      cwd: options.cwd || repoRoot,
      ok: false,
      startedAt,
      stdout: error.stdout?.toString() || '',
      stderr: error.stderr?.toString() || '',
      status: error.status,
    });
    throw error;
  }
}

function writeJson(filePath, value) {
  writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`);
}

function flattenMenus(menus, parent = '') {
  const rows = [];
  for (const menu of menus || []) {
    const fullName = parent ? `${parent} / ${menu.menuName}` : menu.menuName;
    rows.push({ ...menu, fullName });
    rows.push(...flattenMenus(menu.children || [], fullName));
  }
  return rows;
}

function safeName(value) {
  return String(value).replace(/[\\/:*?"<>|\s]+/g, '-').replace(/^-+|-+$/g, '') || 'page';
}

async function waitForHttp(url, timeoutMs = 45000) {
  const started = Date.now();
  let lastError;
  while (Date.now() - started < timeoutMs) {
    try {
      const response = await fetch(url, { cache: 'no-store' });
      if (response.ok) {
        return;
      }
      lastError = new Error(`${url} returned ${response.status}`);
    } catch (error) {
      lastError = error;
    }
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  throw lastError || new Error(`Timed out waiting for ${url}`);
}

async function screenshot(page, name, options = {}) {
  const path = resolve(screenshotDir, name);
  await page.screenshot({ path, fullPage: options.fullPage ?? true });
  screenshots.push(path);
  return path;
}

async function collectShell(page) {
  return {
    topbarVisible: await page.locator('.layout-navbars-container').first().isVisible().catch(() => false),
    asideVisible: await page.locator('.layout-aside').first().isVisible().catch(() => false),
    tagsVisible: await page.locator('.tags-view-container').first().isVisible().catch(() => false),
    noticeBellVisible: await page.locator('.notice-bell, .mango-notice-trigger, [class*="notice"]').first().isVisible().catch(() => false),
    settingsVisible: await page.locator('.layout-breadcrumb-settings').first().isVisible().catch(() => false),
    userVisible: await page.locator('.layout-breadcrumb-user').first().isVisible().catch(() => false),
    topMenuTexts: await page.locator('.layout-top-system-item').evaluateAll(nodes => nodes.map(node => node.textContent?.trim()).filter(Boolean)).catch(() => []),
    sideMenuTexts: await page.locator('.el-menu .el-menu-item, .el-menu .el-sub-menu__title').evaluateAll(nodes => nodes.map(node => node.textContent?.trim()).filter(Boolean)).catch(() => []),
  };
}

async function collectLayout(page) {
  return page.evaluate(() => {
    const selectors = [
      '.layout-container',
      '.layout-aside',
      '.layout-navbars-container',
      '.layout-main',
      '.tags-view-container',
      '.notice-bell',
      '.layout-breadcrumb-settings',
      '.layout-breadcrumb-user',
    ];
    return selectors.map((selector) => {
      const node = document.querySelector(selector);
      if (!node) {
        return { selector, visible: false };
      }
      const rect = node.getBoundingClientRect();
      const style = window.getComputedStyle(node);
      return {
        selector,
        visible: rect.width > 0 && rect.height > 0,
        x: Math.round(rect.x),
        y: Math.round(rect.y),
        width: Math.round(rect.width),
        height: Math.round(rect.height),
        display: style.display,
        color: style.color,
        backgroundColor: style.backgroundColor,
        borderRadius: style.borderRadius,
      };
    });
  });
}

async function login(page) {
  await page.goto(`${frontendUrl}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('.login-container', { timeout: 20000 });
  await screenshot(page, 's5-nexus-login.png', { fullPage: false });
  const tenantRequest = page.waitForResponse(
    response => response.url().includes('/api/auth/login-institutions') && response.status() === 200,
    { timeout: 10000 },
  ).catch(() => undefined);
  await page.locator('input[placeholder="用户名"]').fill(credentials.username);
  await page.locator('input[placeholder="密码"]').fill(credentials.password);
  await page.locator('input[placeholder="密码"]').blur();
  await tenantRequest;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: new RegExp(credentials.tenantNamePattern) }).click();
  const menuResponsePromise = page.waitForResponse(
    response => response.url().includes('/api/authorization/menus/user') && response.url().includes('fmt=tree') && response.status() === 200,
    { timeout: 20000 },
  );
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 20000 });
  const menuResponse = await menuResponsePromise;
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined);
  await page.locator('.el-message').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => undefined);
  return menuResponse;
}

async function getApiState() {
  const api = await playwrightRequest.newContext({ baseURL: backendUrl });
  try {
    const healthResponse = await api.get('/actuator/health');
    const health = await healthResponse.json().catch(() => ({}));
    const loginResponse = await api.post('/auth/login', { data: loginPayload });
    const loginBody = await loginResponse.json();
    const token = loginBody?.data?.accessToken || loginBody?.data?.token;
    if (!loginResponse.ok() || !token) {
      throw new Error(`Backend login failed: ${loginResponse.status()} ${JSON.stringify(loginBody)}`);
    }
    const menuResponse = await api.get('/authorization/menus/user', {
      params: { fmt: 'tree', appCode: 'internal-admin' },
      headers: {
        Authorization: `Bearer ${token}`,
        'X-Tenant-Id': String(loginBody?.data?.tenantId || credentials.tenantCode),
      },
    });
    const menuBody = await menuResponse.json();
    const menus = Array.isArray(menuBody?.data) ? menuBody.data : [];
    return {
      health: { status: healthResponse.status(), body: health },
      login: {
        status: loginResponse.status(),
        success: loginBody?.success,
        code: loginBody?.code,
        hasAccessToken: Boolean(token),
        username: loginBody?.data?.userInfo?.username,
        tenantName: loginBody?.data?.tenantName,
      },
      menu: {
        status: menuResponse.status(),
        success: menuBody?.success,
        code: menuBody?.code,
        topLevelCount: menus.length,
        topLevelMenus: menus.map(menu => menu.menuName),
        totalRows: flattenMenus(menus).length,
      },
      menus,
    };
  } finally {
    await api.dispose();
  }
}

function prepareConsumer() {
  rmSync(workspaceRoot, { recursive: true, force: true });
  mkdirSync(workspaceRoot, { recursive: true });
  cpSync(consumerTemplateDir, appDir, { recursive: true });

  const packageJsonPath = join(appDir, 'package.json');
  const packageJson = JSON.parse(run('node', ['-e', `console.log(JSON.stringify(require(${JSON.stringify(packageJsonPath)}), null, 2))`]));
  packageJson.scripts.dev = `vite --host 127.0.0.1 --port ${frontendPort}`;
  packageJson.dependencies['@mango/admin'] = packageVersion;
  writeJson(packageJsonPath, packageJson);

  writeFileSync(join(appDir, 'vite.config.ts'), `import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: '${backendUrl}',
        changeOrigin: true,
        rewrite: path => path.replace(/^\\/api/, ''),
      },
    },
  },
});
`);
  mkdirSync(join(appDir, 'public'), { recursive: true });
  writeFileSync(join(appDir, 'public/runtime-config.json'), `${JSON.stringify({
    profile: 'monolith',
    modules: {
      'mango-authorization': { mode: 'local', runtimeCode: 'mango-admin-rbac-app', appType: 'LOCAL', framework: 'vue3' },
      'mango-system': { mode: 'local', runtimeCode: 'mango-admin-system-local', appType: 'LOCAL', framework: 'vue3' },
      'mango-workflow': { mode: 'local', runtimeCode: 'mango-admin-workflow-app', appType: 'LOCAL', framework: 'vue3' },
    },
  }, null, 2)}\n`);

  run('npm', ['view', '@mango/admin', 'version', `--registry=${registry}`]);
  run('pnpm', ['install', '--ignore-workspace', `--registry=${registry}`], { cwd: appDir });
}

async function startConsumerServer() {
  devServer = spawn('pnpm', ['run', 'dev'], {
    cwd: appDir,
    env: {
      ...process.env,
      VITE_MANGO_E2E: 'true',
      VITE_MANGO_DEPLOY_ENV: 'dev',
      VITE_MANGO_RBAC_MODE: 'local',
      VITE_MANGO_SYSTEM_MODE: 'local',
      VITE_MANGO_WORKFLOW_MODE: 'local',
    },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  devServer.stdout.on('data', chunk => serverLogs.push(chunk.toString()));
  devServer.stderr.on('data', chunk => serverLogs.push(chunk.toString()));
  await waitForHttp(frontendUrl);
}

async function verifyRuntime(apiState) {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 960 } });
  const page = await context.newPage();
  page.on('console', (message) => {
    const text = message.text();
    if (message.type() === 'error' && !text.includes('ResizeObserver loop')) {
      consoleErrors.push(text);
    }
  });
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('requestfailed', (request) => {
    const url = request.url();
    if (!url.includes('/__vite_ping') && !url.includes('/favicon')) {
      networkFailures.push({ url, failure: request.failure()?.errorText || '' });
    }
  });
  page.on('response', (response) => {
    const url = response.url();
    const status = response.status();
    if (status >= 400 && !url.includes('/favicon') && !url.includes('/notifications/unread-count') && !url.endsWith('/runtime-config.json')) {
      badResponses.push({ url, status });
    }
  });

  try {
    const menuResponse = await login(page);
    await screenshot(page, 's5-nexus-home.png', { fullPage: true });
    const homeShell = await collectShell(page);
    const homeLayout = await collectLayout(page);
    checks.push({
      name: 'home-shell',
      url: page.url(),
      menuResponse: { status: menuResponse.status(), url: menuResponse.url() },
      shell: homeShell,
      layout: homeLayout,
      pass: homeShell.topbarVisible && homeShell.asideVisible && homeShell.tagsVisible && homeShell.noticeBellVisible && homeShell.settingsVisible && homeShell.userVisible,
    });

    await verifyPage(page, {
      name: 'dev-upload',
      path: '/components/upload',
      waitText: 'MUpload 文件上传',
      screenshotName: 's5-nexus-dev-upload.png',
    });
    await verifyPage(page, {
      name: 'dev-workflow-components',
      path: '/components/workflow',
      waitText: '工作流组件',
      screenshotName: 's5-nexus-dev-workflow-components.png',
    });

    const samples = selectMenuSamples(apiState.menus);
    for (const sample of samples) {
      await verifyPage(page, {
        name: `menu-${sample.fullName}`,
        path: sample.path,
        waitText: sample.menuName,
        screenshotName: `s5-nexus-menu-${safeName(sample.fullName)}.png`,
        allowMissingTitle: true,
      });
    }
  } finally {
    await browser.close();
  }
}

function selectMenuSamples(menus) {
  const rows = [];
  for (const top of menus || []) {
    const candidates = flattenMenus(top.children || [], top.menuName)
      .filter(menu => menu.menuType !== 3 && menu.visible !== 0 && menu.path && menu.component)
      .slice(0, 2);
    rows.push(...candidates);
  }
  return rows.slice(0, 8);
}

async function verifyPage(page, options) {
  await page.goto(`${frontendUrl}/#${options.path}`, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined);
  const titleLocator = page.getByText(options.waitText, { exact: false }).first();
  await titleLocator.waitFor({ state: 'visible', timeout: 10000 }).catch(() => undefined);
  const titleVisible = await titleLocator.isVisible().catch(() => false);
  const bodyText = await page.evaluate(() => document.body.innerText);
  const hasErrorText = /404|页面加载失败|Page Not Found|Cannot GET|缺少微应用运行配置|加载菜单失败/.test(bodyText);
  const shell = await collectShell(page);
  const layout = await collectLayout(page);
  const screenshotPath = await screenshot(page, options.screenshotName, { fullPage: true });
  checks.push({
    name: options.name,
    url: page.url(),
    screenshot: screenshotPath,
    titleVisible,
    hasErrorText,
    shell,
    layout,
    pass: (options.allowMissingTitle || titleVisible) && !hasErrorText && shell.topbarVisible && shell.asideVisible && shell.tagsVisible,
  });
}

async function main() {
  const apiState = await getApiState();
  prepareConsumer();
  await startConsumerServer();
  await verifyRuntime(apiState);

  const report = {
    ok: checks.every(item => item.pass) && consoleErrors.length === 0 && pageErrors.length === 0 && networkFailures.length === 0 && badResponses.length === 0,
    checkedAt: new Date().toISOString(),
    registry,
    packageVersion,
    workspaceRoot,
    appDir,
    frontendUrl,
    backendUrl,
    api: {
      health: apiState.health,
      login: apiState.login,
      menu: apiState.menu,
    },
    commands,
    checks,
    screenshots,
    consoleErrors,
    pageErrors,
    networkFailures,
    badResponses,
    serverLogs,
  };
  writeJson(reportPath, report);
  if (!report.ok) {
    throw new Error(`Nexus runtime E2E failed: ${reportPath}`);
  }
  console.log(`Nexus runtime E2E passed. Report: ${reportPath}`);
}

main().catch((error) => {
  writeJson(reportPath, {
    ok: false,
    checkedAt: new Date().toISOString(),
    registry,
    packageVersion,
    workspaceRoot,
    appDir,
    frontendUrl,
    backendUrl,
    commands,
    checks,
    screenshots,
    consoleErrors,
    pageErrors,
    networkFailures,
    badResponses,
    serverLogs,
    error: error.stack || error.message,
  });
  console.error(error);
  process.exitCode = 1;
}).finally(() => {
  if (devServer && !devServer.killed) {
    devServer.kill('SIGTERM');
  }
});
