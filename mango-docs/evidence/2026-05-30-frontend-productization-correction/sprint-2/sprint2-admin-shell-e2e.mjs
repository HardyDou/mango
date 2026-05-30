import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../../../..');
const require = createRequire(path.join(repoRoot, 'mango-ui/apps/mango-admin/package.json'));
const { chromium, request: playwrightRequest } = require('@playwright/test');

const evidenceDir = __dirname;
const screenshotDir = path.join(evidenceDir, 'screenshots');
const frontendUrl = process.env.MANGO_FRONTEND_URL || 'http://127.0.0.1:8490';
const backendUrl = process.env.MANGO_BACKEND_URL || 'http://127.0.0.1:18800';

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
  startedAt: new Date().toISOString(),
  frontendUrl,
  backendUrl,
  screenshots: [],
  consoleErrors: [],
  pageErrors: [],
  networkFailures: [],
};

async function writeJson(fileName, value) {
  await fs.writeFile(path.join(evidenceDir, fileName), `${JSON.stringify(value, null, 2)}\n`, 'utf8');
}

async function screenshot(page, fileName, options = {}) {
  const fullPath = path.join(screenshotDir, fileName);
  await page.screenshot({ path: fullPath, fullPage: options.fullPage ?? false });
  state.screenshots.push(fullPath);
  return fullPath;
}

function flattenMenus(menus, parent = '') {
  const rows = [];
  for (const menu of menus || []) {
    const fullName = parent ? `${parent} / ${menu.menuName}` : menu.menuName;
    rows.push({
      menuId: menu.menuId,
      parentId: menu.parentId,
      menuName: menu.menuName,
      menuCode: menu.menuCode,
      menuType: menu.menuType,
      path: menu.path,
      component: menu.component,
      icon: menu.icon,
      visible: menu.visible,
      status: menu.status,
      fullName,
    });
    rows.push(...flattenMenus(menu.children || [], fullName));
  }
  return rows;
}

async function collectBox(page, selector) {
  return page.locator(selector).first().evaluate((node, currentSelector) => {
    const rect = node.getBoundingClientRect();
    const style = window.getComputedStyle(node);
    return {
      selector: currentSelector,
      visible: rect.width > 0 && rect.height > 0,
      x: Math.round(rect.x),
      y: Math.round(rect.y),
      width: Math.round(rect.width),
      height: Math.round(rect.height),
      color: style.color,
      backgroundColor: style.backgroundColor,
      fontSize: style.fontSize,
    };
  }, selector).catch(() => ({ selector, visible: false }));
}

async function collectText(page, selector) {
  return page.locator(selector).evaluateAll((nodes) => nodes
    .map((node) => node.textContent?.trim())
    .filter(Boolean));
}

async function clickFirstVisible(page, selectors) {
  for (const selector of selectors) {
    const count = await page.locator(selector).count();
    for (let index = 0; index < count; index += 1) {
      const target = page.locator(selector).nth(index);
      if (await target.isVisible().catch(() => false)) {
        await target.click();
        return { selector, index };
      }
    }
  }
  return null;
}

async function closeFloatingLayers(page) {
  await page.keyboard.press('Escape').catch(() => undefined);
  await page.mouse.click(520, 520).catch(() => undefined);
  await page.waitForTimeout(300);
}

async function main() {
  await fs.mkdir(screenshotDir, { recursive: true });

  const api = await playwrightRequest.newContext({ baseURL: backendUrl });
  const health = await api.get('/actuator/health');
  const healthBody = await health.json();
  const loginResponse = await api.post('/auth/login', { data: credentials });
  const loginBody = await loginResponse.json();
  const token = loginBody?.data?.accessToken || loginBody?.data?.token;
  if (!loginResponse.ok() || !token) {
    throw new Error(`Real backend login failed: ${loginResponse.status()} ${JSON.stringify(loginBody)}`);
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
  const backendMenus = Array.isArray(menuBody?.data) ? menuBody.data : [];
  const menuRows = flattenMenus(backendMenus);

  await writeJson('api-report.json', {
    health: { status: health.status(), body: healthBody },
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
      topLevelCount: backendMenus.length,
      topLevelMenus: backendMenus.map((menu) => menu.menuName),
      totalRows: menuRows.length,
    },
  });
  await writeJson('menu-report.json', {
    topLevelMenus: backendMenus.map((menu) => ({
      menuName: menu.menuName,
      path: menu.path,
      icon: menu.icon,
      children: (menu.children || []).map((child) => ({
        menuName: child.menuName,
        path: child.path,
        component: child.component,
        icon: child.icon,
        visible: child.visible,
      })),
    })),
    rows: menuRows,
  });

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
    state.networkFailures.push({
      url: request.url(),
      failure: request.failure()?.errorText,
    });
  });

  await page.goto(`${frontendUrl}/#/login`, { waitUntil: 'networkidle' });
  await page.waitForSelector('.login-container', { timeout: 15000 });
  await screenshot(page, 'login-1440x960.png');

  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200,
  );
  await page.fill('input[placeholder="用户名"]', credentials.username);
  await page.fill('input[placeholder="密码"]', credentials.password);
  await page.locator('input[placeholder="密码"]').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();

  const menuUiResponsePromise = page.waitForResponse((response) => {
    const url = response.url();
    return response.status() === 200
      && url.includes('/api/authorization/menus/user')
      && url.includes('fmt=tree');
  });
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 15000 });
  const menuUiResponse = await menuUiResponsePromise;
  await page.waitForLoadState('networkidle');
  await page.locator('.el-message').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => undefined);
  await page.waitForTimeout(800);
  await screenshot(page, 'home-1440x960.png', { fullPage: true });

  const layoutReport = {
    viewport: page.viewportSize(),
    url: page.url(),
    boxes: [
      await collectBox(page, '.layout-container'),
      await collectBox(page, '.layout-aside'),
      await collectBox(page, '.layout-navbars-container'),
      await collectBox(page, '.layout-main'),
      await collectBox(page, '.tags-view-container'),
      await collectBox(page, '.notice-bell'),
      await collectBox(page, '.layout-breadcrumb-settings'),
      await collectBox(page, '.layout-breadcrumb-user'),
    ],
    topMenuTexts: await collectText(page, '.layout-top-system-item'),
    sideMenuTexts: await collectText(page, '.el-menu .el-menu-item, .el-menu .el-sub-menu__title'),
    noticeBellVisible: await page.locator('.notice-bell').first().isVisible().catch(() => false),
    settingsButtonVisible: await page.locator('.layout-breadcrumb-settings').first().isVisible().catch(() => false),
    userText: await page.locator('.layout-breadcrumb-user').first().textContent().catch(() => ''),
    menuUiResponse: {
      status: menuUiResponse.status(),
      url: menuUiResponse.url(),
    },
  };

  await clickFirstVisible(page, ['.notice-bell']);
  await page.waitForTimeout(700);
  await screenshot(page, 'notice-bell-1440x960.png');
  await closeFloatingLayers(page);

  await clickFirstVisible(page, ['.layout-breadcrumb-user']);
  await page.waitForTimeout(500);
  await screenshot(page, 'user-dropdown-1440x960.png');
  await closeFloatingLayers(page);

  await page.goto(`${frontendUrl}/#/home`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  const settingsClick = await clickFirstVisible(page, ['.layout-breadcrumb-settings']);
  layoutReport.settingsClick = settingsClick;
  if (!settingsClick) {
    throw new Error('Settings button was not visible.');
  }
  await page.waitForSelector('.el-drawer', { state: 'visible', timeout: 5000 });
  await page.waitForTimeout(800);
  await screenshot(page, 'settings-drawer-1440x960.png');
  layoutReport.settingsDrawerVisible = await page.locator('.el-drawer').first().isVisible().catch(() => false);
  layoutReport.settingsDrawerText = await page.locator('.el-drawer').first().textContent().catch(() => '');
  await page.locator('.el-drawer__close-btn').first().click();
  await page.waitForSelector('.el-drawer', { state: 'hidden', timeout: 5000 });

  const menuSampling = [];
  for (const menu of backendMenus.filter((item) => item.menuType !== 3 && item.visible !== 0)) {
    const children = (menu.children || [])
      .filter((child) => child.menuType !== 3 && child.visible !== 0 && child.path)
      .slice(0, 3);
    const samples = [];
    for (const child of children) {
      await page.goto(`${frontendUrl}/#${child.path}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(900);
      const safeName = `${menu.menuName}-${child.menuName}`.replace(/[\\/:*?"<>|\s]+/g, '-');
      const fileName = `sample-${safeName}.png`;
      const screenshotPath = await screenshot(page, fileName, { fullPage: true });
      samples.push({
        menuName: child.menuName,
        path: child.path,
        screenshot: screenshotPath,
        titleVisible: await page.getByText(child.menuName).first().isVisible().catch(() => false),
        errorVisible: await page.locator('text=/401|403|未授权|拒绝访问|路由加载失败|加载失败|404/').first().isVisible().catch(() => false),
      });
    }
    menuSampling.push({
      topLevelMenu: menu.menuName,
      path: menu.path,
      sampledChildren: samples,
    });
  }

  await writeJson('layout-report.json', layoutReport);
  await writeJson('menu-sampling-report.json', {
    firstLevelMenus: backendMenus
      .filter((menu) => menu.menuType !== 3 && menu.visible !== 0)
      .map((menu) => menu.menuName),
    sampling: menuSampling,
  });

  await browser.close();
  await api.dispose();

  state.finishedAt = new Date().toISOString();
  await writeJson('summary.json', state);
  console.log(JSON.stringify({
    frontendUrl,
    backendUrl,
    screenshots: state.screenshots,
    consoleErrors: state.consoleErrors.length,
    pageErrors: state.pageErrors.length,
    networkFailures: state.networkFailures.length,
  }, null, 2));
}

main().catch(async (error) => {
  await fs.mkdir(evidenceDir, { recursive: true });
  await fs.writeFile(path.join(evidenceDir, 'failure.log'), `${error.stack || error.message}\n`, 'utf8');
  console.error(error);
  process.exit(1);
});
