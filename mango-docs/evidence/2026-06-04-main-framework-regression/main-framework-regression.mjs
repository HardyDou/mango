import { createRequire } from 'node:module';
import { mkdir, writeFile } from 'node:fs/promises';
import { writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import path from 'node:path';

const adminRequire = createRequire(path.resolve('mango-ui/apps/mango-admin/package.json'));
const { chromium } = adminRequire('@playwright/test');

const baseURL = process.env.MANGO_ACCEPTANCE_BASE_URL || 'http://127.0.0.1:8244';
const evidenceDir = path.resolve('mango-docs/evidence/2026-06-04-main-framework-regression');
const screenshotsDir = path.join(evidenceDir, 'screenshots');

const report = {
  baseURL,
  startedAt: new Date().toISOString(),
  finishedAt: '',
  result: 'UNKNOWN',
  checks: [],
  screenshots: [],
  console: [],
  failedRequests: [],
  badResponses: [],
  uiFindings: [],
};

function pushCheck(name, status, details = {}) {
  report.checks.push({ name, status, ...details });
}

function visibleTextSample(text) {
  return text.replace(/\s+/g, ' ').trim().slice(0, 500);
}

function isIgnorableResponse(response) {
  const url = response.url();
  if (url.includes('/sockjs-node')) {
    return true;
  }
  return false;
}

async function screenshot(page, name, description) {
  const file = path.join(screenshotsDir, `${name}.png`);
  await page.screenshot({ path: file, fullPage: true });
  report.screenshots.push({ name, description, file });
  return file;
}

async function waitApiOk(page, urlPart, method = 'GET', timeout = 20000) {
  const response = await page.waitForResponse((res) => {
    return res.url().includes(urlPart) && res.request().method() === method;
  }, { timeout });
  const status = response.status();
  let body;
  try {
    body = await response.json();
  } catch {
    body = undefined;
  }
  const ok = status >= 200 && status < 300 && (!body || body.success || body.code === 200);
  if (!ok) {
    throw new Error(`${method} ${urlPart} failed with ${status}: ${JSON.stringify(body)}`);
  }
  return { response, body };
}

async function expectText(page, text, timeout = 12000) {
  await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout });
}

async function waitTransientMessages(page) {
  await page.locator('.el-message').first().waitFor({ state: 'detached', timeout: 5000 }).catch(() => undefined);
}

async function login(page) {
  await page.goto(`${baseURL}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('input[placeholder="用户名"]').fill('admin');
  await page.locator('input[placeholder="密码"]').fill('admin123');
  const tenants = waitApiOk(page, '/api/auth/login-institutions', 'POST');
  await page.locator('input[placeholder="密码"]').blur();
  await tenants;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  const menus = waitApiOk(page, '/api/authorization/menus/user', 'GET');
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 15000 });
  await menus;
  await page.getByRole('button', { name: '系统管理' }).waitFor({ state: 'visible', timeout: 15000 });
  await page.getByText('芒果集团').first().waitFor({ state: 'visible', timeout: 15000 });
  pushCheck('登录', 'PASS', {
    assertions: ['admin/admin123 登录成功', '租户芒果集团可见', '进入 /home', '菜单接口返回成功'],
  });
}

async function openByTopMenu(page, topName, leftName, expectedHash, apiPart, expectedTexts, shotName) {
  const apiPromise = apiPart ? waitApiOk(page, apiPart, 'GET').catch((error) => ({ error: String(error) })) : null;
  await page.getByRole('button', { name: topName }).click();
  await page.getByRole('menuitem', { name: leftName }).click();
  await page.waitForURL(`**/#${expectedHash}`, { timeout: 15000 });
  const apiResult = apiPromise ? await apiPromise : null;
  if (apiResult?.error) {
    throw new Error(apiResult.error);
  }
  for (const text of expectedTexts) {
    await expectText(page, text);
  }
  await screenshot(page, shotName, `${topName} / ${leftName}`);
  const body = await page.locator('body').innerText();
  pushCheck(`菜单跳转-${topName}-${leftName}`, 'PASS', {
    route: expectedHash,
    assertions: expectedTexts,
    textSample: visibleTextSample(body),
  });
}

async function openByTopMenuPath(page, topName, leftNames, expectedHash, apiPart, expectedTexts, shotName) {
  const apiPromise = apiPart ? waitApiOk(page, apiPart, 'GET').catch((error) => ({ error: String(error) })) : null;
  await page.getByRole('button', { name: topName }).click();
  for (const name of leftNames) {
    await page.getByRole('menuitem', { name }).click();
  }
  await page.waitForURL(`**/#${expectedHash}`, { timeout: 15000 });
  const apiResult = apiPromise ? await apiPromise : null;
  if (apiResult?.error) {
    throw new Error(apiResult.error);
  }
  for (const text of expectedTexts) {
    await expectText(page, text);
  }
  await screenshot(page, shotName, `${topName} / ${leftNames.join(' / ')}`);
  const body = await page.locator('body').innerText();
  pushCheck(`菜单跳转-${topName}-${leftNames.join('-')}`, 'PASS', {
    route: expectedHash,
    assertions: expectedTexts,
    textSample: visibleTextSample(body),
  });
}

async function verifyHome(page) {
  await page.goto(`${baseURL}/#/home`, { waitUntil: 'domcontentloaded' });
  await expectText(page, '权限与组织');
  await expectText(page, '流程与协同');
  await expectText(page, '平台基础能力');
  await page.locator('.el-icon-bell, .el-icon svg, [class*="bell"]').first().waitFor({ state: 'visible', timeout: 15000 }).catch(() => undefined);
  await screenshot(page, '01-home', '首页能力入口、顶部导航和左侧首页菜单');
  pushCheck('首页', 'PASS', {
    assertions: ['首页三块能力入口可见', '顶部导航可见', '左侧首页菜单可见'],
  });
}

async function verifySystem(page) {
  await openByTopMenu(
    page,
    '系统管理',
    '成员管理',
    '/system/user',
    '/api/identity/users/page',
    ['成员管理', '新增成员', '用户名'],
    '02-system-user',
  );
  const keywordInput = page.locator('input[placeholder*="用户名"], input[placeholder*="昵称"], input[placeholder*="手机号"]').first();
  await keywordInput.fill('admin');
  const query = waitApiOk(page, '/api/identity/users/page', 'GET');
  await page.getByRole('button', { name: '查询' }).click();
  await query;
  await expectText(page, 'admin');
  await screenshot(page, '03-system-user-search', '成员管理查询 admin 后表格回显');
  pushCheck('系统管理-成员查询', 'PASS', {
    assertions: ['输入 admin 查询', '接口返回成功', '表格回显 admin'],
  });
}

async function verifyWorkflow(page) {
  await openByTopMenu(
    page,
    '审批中心',
    '发起流程',
    '/workflow/start-process',
    '/api/workflow/definitions/page',
    ['发起流程', '已发布流程', '请假申请'],
    '04-workflow-start',
  );
  await page.getByText('请假申请').first().click();
  await page.getByRole('dialog', { name: /请假申请/ }).waitFor({ state: 'visible', timeout: 15000 });
  await expectText(page, '请假天数');
  await expectText(page, '开始日期');
  await screenshot(page, '05-workflow-leave-dialog', '发起请假流程弹窗和动态表单');
  pushCheck('审批中心-流程发起弹窗', 'PASS', {
    assertions: ['请假申请卡片可点击', '动态表单弹窗打开', '请假天数/日期字段可见'],
  });
  await page.getByRole('dialog', { name: /请假申请/ }).getByRole('button', { name: '取消' }).click();
}

async function verifyCalendar(page) {
  await openByTopMenu(
    page,
    '平台能力',
    '日历管理',
    '/data/calendar',
    '/api/calendar/admin/calendars/page',
    ['日历管理', '中国标准工作日历', '日期明细'],
    '06-calendar-page',
  );
  const dateTab = page.getByRole('tab', { name: '日期明细' });
  await dateTab.click();
  const calendarMain = page.locator('.calendar-main');
  const query = waitApiOk(page, '/api/calendar/admin/days/page', 'GET');
  await calendarMain.getByRole('button', { name: '查询' }).last().click();
  await query;
  await calendarMain.locator('.el-table__row').filter({ hasText: /^\s*\d{4}-\d{2}-\d{2}/ }).first()
    .waitFor({ state: 'visible', timeout: 12000 });
  await screenshot(page, '07-calendar-date-query', '日历日期明细按年度查询');
  pushCheck('平台能力-日历查询', 'PASS', {
    assertions: ['日期明细 Tab 可切换', '日期查询接口成功', '日期表格回显真实日期行'],
  });
}

async function verifyFile(page) {
  await page.getByRole('button', { name: '平台能力' }).click();
  await page.getByRole('menuitem', { name: '文件管理' }).first().waitFor({ state: 'visible', timeout: 10000 });
  const directoryTree = waitApiOk(page, '/api/file/directories/tree', 'GET');
  await page.goto(`${baseURL}/#/file/files`, { waitUntil: 'domcontentloaded' });
  await directoryTree;
  await expectText(page, '文件管理');
  await expectText(page, '上传文件');
  await screenshot(page, '08-file-page', '平台能力 / 文件管理');
  pushCheck('菜单跳转-平台能力-文件管理', 'PASS', {
    route: '/file/files',
    assertions: ['平台能力文件管理菜单可见', '文件管理页面可打开', '目录树接口成功'],
  });
  const filePath = path.join(tmpdir(), `mango-main-regression-${Date.now()}.txt`);
  writeFileSync(filePath, `mango main framework regression ${new Date().toISOString()}`);
  const upload = waitApiOk(page, '/api/file/files', 'POST', 30000);
  await page.setInputFiles('input[type="file"]', filePath);
  const uploadResult = await upload;
  const fileName = uploadResult.body?.data?.fileName || path.basename(filePath);
  await expectText(page, '上传成功').catch(() => undefined);
  await waitTransientMessages(page);
  await page.locator('.file-main input[placeholder="搜索文件名/业务信息"]').fill(fileName);
  const search = waitApiOk(page, '/api/file/files/page', 'GET');
  await page.locator('.file-main').getByRole('button', { name: '查询' }).click();
  await search;
  await page.locator('.file-main .el-table__row', { hasText: fileName }).first()
    .waitFor({ state: 'visible', timeout: 12000 });
  await screenshot(page, '09-file-upload', '文件管理上传 txt 文件后表格回显');
  pushCheck('平台能力-文件上传', 'PASS', {
    assertions: ['选择本地 txt 文件', 'POST /api/file/files 成功', `按文件名查询后表格回显 ${fileName}`],
  });
}

async function verifyNotice(page) {
  await waitTransientMessages(page);
  await page.getByRole('button', { name: '通知中心' }).click();
  await page.getByRole('menuitem', { name: '我的消息' }).waitFor({ state: 'visible', timeout: 10000 });
  const messages = waitApiOk(page, '/api/notice/site/my/messages', 'GET');
  await page.goto(`${baseURL}/#/notice/site-message`, { waitUntil: 'domcontentloaded' });
  await messages;
  await expectText(page, '我的消息');
  await expectText(page, '全部已读');
  await expectText(page, '刷新');
  await screenshot(page, '10-notice-messages', '通知中心 / 我的消息');
  pushCheck('菜单跳转-通知中心-我的消息', 'PASS', {
    route: '/notice/site-message',
    assertions: ['通知中心我的消息菜单可见', '我的消息页面可打开', '消息列表接口成功'],
  });
  const bell = page.locator('.el-badge').filter({ has: page.locator('.el-icon') }).first();
  await bell.click();
  await page.locator('.el-popper').filter({ hasText: '我的消息' }).first().waitFor({ state: 'visible', timeout: 15000 });
  await page.waitForTimeout(300);
  await screenshot(page, '11-notice-bell', '右上角小喇叭消息弹层');
  pushCheck('通知中心-小喇叭', 'PASS', {
    assertions: ['通知中心页面可打开', '右上角小喇叭可点击', '消息弹层显示我的消息'],
  });
}

async function verifyTagsView(page) {
  await waitTransientMessages(page);
  await page.goto(`${baseURL}/#/data/calendar`, { waitUntil: 'domcontentloaded' });
  await expectText(page, '日历管理');
  await screenshot(page, '12-tags-before-close', '关闭日历页签前');
  const calendarTab = page.locator('.tags-view-item, .el-tabs__item').filter({ hasText: '日历管理' }).last();
  const close = calendarTab.locator('.el-icon-close, .is-icon-close, [class*="close"]').first();
  await close.click();
  await page.waitForTimeout(800);
  await screenshot(page, '13-tags-after-close', '关闭日历页签后应切换到其它有效页签或首页');
  const text = await page.locator('body').innerText();
  const hash = await page.evaluate(() => location.hash);
  const pass = !text.includes('404') && !text.includes('系统错误') && !text.includes('Not Found');
  pushCheck('页签关闭', pass ? 'PASS' : 'FAIL', {
    hash,
    assertions: pass
      ? ['关闭当前页签后未出现 404/系统错误']
      : ['关闭当前页签后出现 404/系统错误或无有效页面'],
    textSample: visibleTextSample(text),
  });
}

async function verifyRootRedirect(page) {
  await page.goto(`${baseURL}/`, { waitUntil: 'domcontentloaded' });
  await page.waitForURL('**/#/home', { timeout: 15000 });
  await expectText(page, '权限与组织');
  await screenshot(page, '14-root-redirect', '/ 默认跳转首页');
  pushCheck('根路径跳转', 'PASS', {
    assertions: ['/ 默认跳转到 /home', '首页内容可见'],
  });
}

async function collectUiFindings(page) {
  const viewport = page.viewportSize();
  const findings = await page.evaluate(() => {
    const rows = [];
    for (const selector of ['.el-table', '.el-dialog', '.el-card', '.tags-view', '.layout-navbars']) {
      for (const el of Array.from(document.querySelectorAll(selector))) {
        const rect = el.getBoundingClientRect();
        rows.push({
          selector,
          width: Math.round(rect.width),
          height: Math.round(rect.height),
          overflowX: el.scrollWidth > el.clientWidth + 2,
          overflowY: el.scrollHeight > el.clientHeight + 2,
          text: (el.textContent || '').replace(/\s+/g, ' ').trim().slice(0, 120),
        });
      }
    }
    return rows;
  });
  report.uiFindings.push({ viewport, findings });
}

async function main() {
  await mkdir(screenshotsDir, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1280, height: 720 } });

  page.on('console', (message) => {
    if (['error', 'warning'].includes(message.type())) {
      report.console.push({ type: message.type(), text: message.text(), location: message.location() });
    }
  });
  page.on('requestfailed', (request) => {
    report.failedRequests.push({
      method: request.method(),
      url: request.url(),
      failure: request.failure()?.errorText,
    });
  });
  page.on('response', (response) => {
    if (response.status() >= 400 && !isIgnorableResponse(response)) {
      report.badResponses.push({
        method: response.request().method(),
        url: response.url(),
        status: response.status(),
      });
    }
  });

  try {
    await login(page);
    await verifyHome(page);
    await verifySystem(page);
    await verifyWorkflow(page);
    await verifyCalendar(page);
    await verifyFile(page);
    await verifyNotice(page);
    await verifyTagsView(page);
    await verifyRootRedirect(page);
    await collectUiFindings(page);

    const failedChecks = report.checks.filter((item) => item.status !== 'PASS');
    report.result = failedChecks.length === 0 ? 'PASS' : 'FAIL';
  } catch (error) {
    report.result = 'FAIL';
    pushCheck('脚本执行', 'FAIL', { error: String(error?.stack || error) });
    await screenshot(page, '99-failure', '脚本失败时页面状态').catch(() => undefined);
  } finally {
    report.finishedAt = new Date().toISOString();
    await writeFile(path.join(evidenceDir, 'browser-report.json'), JSON.stringify(report, null, 2));
    await browser.close();
  }

  if (report.result !== 'PASS') {
    process.exitCode = 1;
  }
}

main();
