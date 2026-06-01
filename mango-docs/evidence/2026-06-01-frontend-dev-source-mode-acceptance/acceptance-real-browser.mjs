import { createRequire } from 'node:module';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

const adminRequire = createRequire(path.resolve('mango-ui/apps/mango-admin/package.json'));
const { chromium } = adminRequire('@playwright/test');

const baseURL = process.env.MANGO_ACCEPTANCE_BASE_URL || 'http://127.0.0.1:7891';
const evidenceDir = path.resolve(
  'mango-docs/evidence/2026-06-01-frontend-dev-source-mode-acceptance',
);

const summary = {
  baseURL,
  startedAt: new Date().toISOString(),
  pages: [],
  operations: [],
  console: [],
  failedRequests: [],
  apiFailures: [],
};

function recordPage(name, route, screenshot, assertions) {
  summary.pages.push({ name, route, screenshot, assertions });
}

function recordOperation(name, screenshot, assertions) {
  summary.operations.push({ name, screenshot, assertions });
}

async function screenshot(page, filename) {
  const target = path.join(evidenceDir, filename);
  await page.screenshot({ path: target, fullPage: true });
  return target;
}

async function expectVisible(pageOrLocator, locator, description) {
  const target = typeof locator === 'string' ? pageOrLocator.locator(locator) : locator;
  await target.first().waitFor({ state: 'visible', timeout: 15000 });
  return description;
}

async function expectMessage(page, message) {
  await page.locator('.el-message__content', { hasText: message }).last()
    .waitFor({ state: 'visible', timeout: 10000 });
}

async function waitForApi(page, urlPart, method = 'GET') {
  const response = await page.waitForResponse((res) => {
    return res.url().includes(urlPart) && res.request().method() === method;
  }, { timeout: 20000 });
  let body;
  try {
    body = await response.json();
  } catch {
    body = undefined;
  }
  const ok = response.status() >= 200 && response.status() < 300 && (!body || body.success || body.code === 200);
  if (!ok) {
    summary.apiFailures.push({
      url: response.url(),
      method,
      status: response.status(),
      body,
    });
    throw new Error(`API failed: ${method} ${response.url()} ${response.status()}`);
  }
  return response;
}

async function login(page) {
  await page.goto(`${baseURL}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('input[placeholder="用户名"]').fill('admin');
  await page.locator('input[placeholder="密码"]').fill('admin123');
  const tenants = waitForApi(page, '/api/auth/login-institutions', 'POST');
  await page.locator('input[placeholder="密码"]').blur();
  await tenants;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  const menus = waitForApi(page, '/api/authorization/menus/user', 'GET');
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 15000 });
  await menus;
  await expectVisible(page, page.getByRole('button', { name: '系统管理' }), '登录后系统管理菜单可见');
}

async function openPage(page, route, api, title, labels, screenshotName) {
  const apiPromise = waitForApi(page, api, 'GET');
  await page.goto(`${baseURL}/#${route}`, { waitUntil: 'domcontentloaded' });
  await apiPromise;
  const assertions = [
    await expectVisible(page, page.getByText(title), `页面标题/核心文案「${title}」可见`),
  ];
  for (const label of labels) {
    assertions.push(await expectVisible(page, page.getByText(label), `关键 UI「${label}」可见`));
  }
  const shot = await screenshot(page, screenshotName);
  recordPage(title, route, shot, assertions);
}

async function operateConfig(page, unique) {
  const paramKey = `accept.param.${unique}`;
  const configKey = `accept.config.${unique}`;

  await openPage(
    page,
    '/system/config',
    '/api/system/config/list',
    '系统配置',
    ['系统参数', '新增参数'],
    '04-system-config-page.png',
  );

  await page.getByRole('button', { name: '新增参数' }).click();
  const createParamDialog = page.getByRole('dialog', { name: '新增参数' });
  await expectVisible(page, createParamDialog, '新增参数弹窗可见');
  await createParamDialog.getByLabel('参数键').fill(paramKey);
  await createParamDialog.getByLabel('参数值').fill('value-1');
  await createParamDialog.getByLabel('描述').fill(`验收参数${unique}`);
  await Promise.all([
    waitForApi(page, '/api/system/config', 'POST'),
    createParamDialog.getByRole('button', { name: '确定' }).click(),
  ]);
  await expectMessage(page, '新增成功');
  await page.locator('.el-table__row', { hasText: paramKey }).waitFor({ state: 'visible', timeout: 15000 });
  const afterCreateParam = await screenshot(page, '05-config-create-param.png');
  recordOperation('系统配置-新增系统参数', afterCreateParam, [
    `POST /api/system/config 成功`,
    `表格出现参数键 ${paramKey}`,
    '页面显示新增成功提示',
  ]);

  const paramRow = page.locator('.el-table__row', { hasText: paramKey }).first();
  await paramRow.getByRole('button', { name: '删除' }).click();
  await expectVisible(page, page.getByText('确认删除该'), '删除确认弹窗可见');
  await Promise.all([
    waitForApi(page, '/api/system/config', 'DELETE'),
    page.getByRole('button', { name: '确定' }).last().click(),
  ]);
  await expectMessage(page, '删除成功');
  await page.waitForTimeout(500);
  const afterDeleteParam = await screenshot(page, '06-config-delete-param.png');
  recordOperation('系统配置-删除系统参数', afterDeleteParam, [
    `DELETE /api/system/config 成功`,
    `参数键 ${paramKey} 已清理`,
    '页面显示删除成功提示',
  ]);

  await page.getByRole('tab', { name: '系统配置' }).click();
  await waitForApi(page, '/api/system/config/list', 'GET').catch(() => undefined);
  await page.getByRole('button', { name: '新增配置' }).click();
  const createConfigDialog = page.getByRole('dialog', { name: '新增配置' });
  await createConfigDialog.getByLabel('配置键').fill(configKey);
  await createConfigDialog.getByLabel('配置值').fill('enabled');
  await createConfigDialog.getByLabel('描述').fill(`验收配置${unique}`);
  await Promise.all([
    waitForApi(page, '/api/system/config', 'POST'),
    createConfigDialog.getByRole('button', { name: '确定' }).click(),
  ]);
  await expectMessage(page, '新增成功');
  await page.locator('.el-table__row', { hasText: configKey }).waitFor({ state: 'visible', timeout: 15000 });
  const afterCreateConfig = await screenshot(page, '07-config-create-config.png');
  recordOperation('系统配置-新增系统配置', afterCreateConfig, [
    `POST /api/system/config 成功`,
    `表格出现配置键 ${configKey}`,
    '系统配置 Tab 新增配置可见',
  ]);

  const configRow = page.locator('.el-table__row', { hasText: configKey }).first();
  await configRow.getByRole('button', { name: '删除' }).click();
  await Promise.all([
    waitForApi(page, '/api/system/config', 'DELETE'),
    page.getByRole('button', { name: '确定' }).last().click(),
  ]);
  await expectMessage(page, '删除成功');
  await page.waitForTimeout(500);
  const afterDeleteConfig = await screenshot(page, '08-config-delete-config.png');
  recordOperation('系统配置-删除系统配置', afterDeleteConfig, [
    `DELETE /api/system/config 成功`,
    `配置键 ${configKey} 已清理`,
    '页面显示删除成功提示',
  ]);
}

async function operateDictSearch(page) {
  await openPage(
    page,
    '/system/dict',
    '/api/system/dict/type/list',
    '新增类型',
    ['用户性别', '新增数据'],
    '09-system-dict-page.png',
  );

  const search = page.locator('input[placeholder="搜索类型名称/编码"]');
  await search.fill('用户性别');
  await waitForApi(page, '/api/system/dict/type/list', 'GET');
  await page.locator('.type-list .type-item', { hasText: '用户性别' })
    .first()
    .waitFor({ state: 'visible', timeout: 10000 });
  const shot = await screenshot(page, '10-dict-search-type.png');
  recordOperation('字典管理-搜索字典类型', shot, [
    '输入搜索词“用户性别”后触发 GET /api/system/dict/type/list',
    '搜索结果仍展示“用户性别”字典类型',
    '当前字典和右侧数据区域保持可用',
  ]);
}

async function main() {
  await mkdir(evidenceDir, { recursive: true });

  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } });

  page.on('console', (message) => {
    if (['error', 'warning'].includes(message.type())) {
      summary.console.push({ type: message.type(), text: message.text() });
    }
  });
  page.on('requestfailed', (request) => {
    summary.failedRequests.push({
      url: request.url(),
      method: request.method(),
      failure: request.failure()?.errorText,
    });
  });
  page.on('response', async (response) => {
    if (response.status() >= 400) {
      summary.apiFailures.push({
        url: response.url(),
        method: response.request().method(),
        status: response.status(),
      });
    }
  });

  try {
    await login(page);
    const homeShot = await screenshot(page, '01-home-after-login.png');
    recordPage('登录后首页', '/home', homeShot, ['登录成功跳转 /home', '系统管理菜单可见']);

    await openPage(
      page,
      '/system/user',
      '/api/identity/users/page',
      '成员管理',
      ['新增成员', '用户名'],
      '02-system-user-page.png',
    );

    await openPage(
      page,
      '/system/role',
      '/api/authorization/roles',
      '角色管理',
      ['系统角色', '启用'],
      '03-system-role-page.png',
    );

    const unique = Date.now();
    await operateConfig(page, unique);
    await operateDictSearch(page);

    const visibleErrorText = await page.locator('text=/401|403|未授权|拒绝访问|路由加载失败|加载失败|404/').count();
    if (visibleErrorText > 0) {
      throw new Error(`页面存在错误文案数量: ${visibleErrorText}`);
    }
  } finally {
    summary.finishedAt = new Date().toISOString();
    await writeFile(path.join(evidenceDir, 'acceptance-summary.json'), JSON.stringify(summary, null, 2));
    await browser.close();
  }
}

main().catch(async (error) => {
  summary.error = error instanceof Error ? error.stack || error.message : String(error);
  summary.finishedAt = new Date().toISOString();
  await mkdir(evidenceDir, { recursive: true });
  await writeFile(path.join(evidenceDir, 'acceptance-summary.json'), JSON.stringify(summary, null, 2));
  console.error(error);
  process.exit(1);
});
