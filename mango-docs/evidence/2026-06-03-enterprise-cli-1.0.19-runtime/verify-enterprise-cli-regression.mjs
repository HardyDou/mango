import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { execFileSync } from 'node:child_process';

const evidenceDir = '/Users/hardy/Work/mango/mango-docs/evidence/2026-06-03-enterprise-cli-1.0.19-runtime';
const screenshotDir = path.join(evidenceDir, 'regression-screenshots');
const projectRequire = createRequire('/tmp/mango-enterprise-cli-1.0.19-184430/procurement-platform/frontend/package.json');
const { chromium } = projectRequire('@playwright/test');

const report = {
  baseUrl: 'http://127.0.0.1:19081',
  backendUrl: 'http://127.0.0.1:19080',
  database: 'procurement-platform-1019',
  checks: [],
  knownIssues: [],
  console: [],
  failedRequests: [],
  screenshots: [],
};

function check(name, passed, details = {}) {
  report.checks.push({ name, passed, ...details });
  if (!passed) {
    throw new Error(`${name} failed: ${JSON.stringify(details)}`);
  }
}

function knownIssue(name, issueUrl, details = {}) {
  report.knownIssues.push({ name, issueUrl, ...details });
}

async function screenshot(page, name) {
  const file = path.join(screenshotDir, `${name}.png`);
  await page.screenshot({ path: file, fullPage: true });
  report.screenshots.push(file);
  return file;
}

function mysqlScalar(sql) {
  return execFileSync('mysql', ['-uroot', '-D', report.database, '-N', '-e', sql], {
    encoding: 'utf8',
  }).trim();
}

async function pageText(page) {
  return page.locator('body').innerText();
}

async function assertShellChrome(page, name) {
  const text = await pageText(page);
  const hasSpeaker = await page.locator('.notice-bell, button[aria-label="消息提醒"]').count();
  const visible404 = await page.locator('.error-page, .not-found').count()
    + await page.getByText(/页面不存在|访问的页面不存在|404 Not Found/).count();
  check(`${name}-shell-chrome`, text.includes('Mango')
    && text.includes('芒果集团')
    && text.includes('首页')
    && hasSpeaker > 0
    && visible404 === 0
    && !text.includes('系统错误'), {
    url: page.url(),
    hasSpeaker,
    visible404,
    textSample: text.trim().slice(0, 240),
  });
}

await fs.mkdir(screenshotDir, { recursive: true });

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });

page.on('console', (msg) => {
  const type = msg.type();
  const text = msg.text();
  if (['error', 'warning'].includes(type)) {
    report.console.push({ type, text });
  }
});

page.on('requestfailed', (request) => {
  report.failedRequests.push({
    url: request.url(),
    method: request.method(),
    failure: request.failure()?.errorText,
  });
});

try {
  await page.goto(report.baseUrl, { waitUntil: 'networkidle' });
  await page.getByPlaceholder(/账号|用户名|请输入账号|请输入用户名/).fill('admin');
  await page.getByPlaceholder(/密码|请输入密码/).fill('admin123');
  await page.getByRole('button', { name: /登\s*录/ }).click();
  await page.waitForURL(/#\/home/, { timeout: 15000 });
  await page.waitForFunction(() => document.body.innerText.includes('常用能力'), null, { timeout: 10000 });
  await screenshot(page, '01-home-stable');
  await assertShellChrome(page, 'home');
  const homeText = await pageText(page);
  check('home-content-visible', homeText.includes('权限与组织')
    && homeText.includes('流程与协同')
    && homeText.includes('平台基础能力')
    && homeText.includes('常用能力'), {
    url: page.url(),
  });

  await page.goto(`${report.baseUrl}/#/procurement`, { waitUntil: 'networkidle' });
  await page.waitForURL(/#\/procurement\/orders/, { timeout: 10000 });
  await page.waitForFunction(() => document.body.innerText.includes('Order管理'), null, { timeout: 10000 });
  await screenshot(page, '02-procurement-directory-redirect');
  await assertShellChrome(page, 'procurement');
  check('procurement-directory-redirect', page.url().includes('/procurement/orders'), { url: page.url() });

  const orderName = `验收采购订单-${Date.now()}`;
  const orderInput = page.getByPlaceholder('Order名称');
  await orderInput.fill(orderName);
  await page.getByRole('button', { name: '新增' }).click();
  await page.waitForFunction(name => document.body.innerText.includes(name), orderName, { timeout: 10000 });
  const dbCount = mysqlScalar(`SELECT COUNT(*) FROM procurement_order WHERE name='${orderName.replaceAll("'", "''")}'`);
  check('order-create-persisted', Number(dbCount) >= 1, { orderName, dbCount });
  await screenshot(page, '03-order-create');

  await orderInput.fill(orderName);
  await page.getByRole('button', { name: '查询' }).click();
  await page.waitForTimeout(800);
  const orderSearchText = await pageText(page);
  check('order-search-visible', orderSearchText.includes(orderName), { orderName });
  await screenshot(page, '04-order-search');

  await page.goto(`${report.baseUrl}/#/system/dict`, { waitUntil: 'networkidle' });
  await page.waitForFunction(() => document.body.innerText.includes('字典管理'), null, { timeout: 10000 });
  await assertShellChrome(page, 'system-dict');
  await screenshot(page, '05-system-dict');
  const dictInput = page.getByPlaceholder('搜索类型名称/编码');
  await dictInput.fill('用户性别');
  await page.getByRole('button', { name: '查询' }).last().click();
  await page.waitForTimeout(800);
  const dictSearchText = await pageText(page);
  check('dict-search-visible', dictSearchText.includes('用户性别') && dictSearchText.includes('sys_user_sex'), {
    textSample: dictSearchText.trim().slice(0, 240),
  });
  await page.getByRole('button', { name: '重置' }).last().click();
  await page.waitForTimeout(500);
  const dictInputAfterReset = await dictInput.inputValue();
  if (dictInputAfterReset !== '') {
    knownIssue('dict-reset-clears-keyword', 'https://github.com/HardyDou/mango/issues/75', {
      value: dictInputAfterReset,
    });
  } else {
    check('dict-reset-clears-keyword', true, { value: dictInputAfterReset });
  }
  await screenshot(page, '06-system-dict-search-reset');

  await page.goto(`${report.baseUrl}/#/notice/site-message`, { waitUntil: 'networkidle' });
  await page.waitForFunction(() => document.body.innerText.includes('我的消息'), null, { timeout: 10000 });
  await assertShellChrome(page, 'notice-site-message');
  const noticeText = await pageText(page);
  check('notice-page-visible', noticeText.includes('批量已读')
    && noticeText.includes('全部已读')
    && noticeText.includes('查询')
    && noticeText.includes('重置'), {
    textSample: noticeText.trim().slice(0, 240),
  });
  await screenshot(page, '07-notice-site-message');

  await page.goto(`${report.baseUrl}/#/procurement/orders`, { waitUntil: 'networkidle' });
  await page.waitForFunction(() => document.body.innerText.includes('Order管理'), null, { timeout: 10000 });
  const closeIcon = page.locator('.tags-view-item.active .close-icon');
  await closeIcon.click();
  await page.waitForTimeout(1200);
  const afterCloseText = await pageText(page);
  await screenshot(page, '08-after-closing-active-order-tag');
  check('active-tag-close-fallback', !page.url().includes('/procurement/orders')
    && (afterCloseText.includes('我的消息') || afterCloseText.includes('常用能力'))
    && !afterCloseText.includes('系统错误')
    && !afterCloseText.includes('404'), {
    url: page.url(),
    textSample: afterCloseText.trim().slice(0, 240),
  });

  check('no-console-errors-or-warnings', report.console.length === 0, {
    console: report.console,
  });
  check('no-network-failures', report.failedRequests.length === 0, {
    failedRequests: report.failedRequests,
  });
} finally {
  await fs.writeFile(path.join(evidenceDir, 'browser-regression-report.json'), JSON.stringify(report, null, 2));
  await browser.close();
}
