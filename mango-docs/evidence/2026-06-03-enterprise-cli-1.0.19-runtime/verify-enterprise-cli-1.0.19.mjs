import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { execFileSync } from 'node:child_process';

const evidenceDir = '/Users/hardy/Work/mango/mango-docs/evidence/2026-06-03-enterprise-cli-1.0.19-runtime';
const screenshotDir = path.join(evidenceDir, 'screenshots');
const projectRequire = createRequire('/tmp/mango-enterprise-cli-1.0.19-184430/procurement-platform/frontend/package.json');
const { chromium } = projectRequire('@playwright/test');

const report = {
  baseUrl: 'http://127.0.0.1:19081',
  backendUrl: 'http://127.0.0.1:19080',
  database: 'procurement-platform-1019',
  checks: [],
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

async function screenshot(page, name) {
  const file = path.join(screenshotDir, `${name}.png`);
  await page.screenshot({ path: file, fullPage: true });
  report.screenshots.push(file);
  return file;
}

function mysqlScalar(sql) {
  return execFileSync('mysql', ['-uroot', '-D', 'procurement-platform-1019', '-N', '-e', sql], {
    encoding: 'utf8',
  }).trim();
}

await fs.mkdir(screenshotDir, { recursive: true });

const menuRows = mysqlScalar(`
  SELECT COUNT(*)
  FROM authorization_menu
  WHERE menu_code='procurement'
    AND menu_type=1
    AND path='/procurement'
    AND redirect='/procurement/orders'
    AND component IS NULL
`);
check('procurement-directory-menu-data', Number(menuRows) === 1, { menuRows });

const childRows = mysqlScalar(`
  SELECT COUNT(*)
  FROM authorization_menu
  WHERE menu_code='procurement:order:list'
    AND menu_type=2
    AND path='/procurement/orders'
    AND component='procurement/order/index'
`);
check('procurement-order-page-menu-data', Number(childRows) === 1, { childRows });

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });

page.on('console', (msg) => {
  const type = msg.type();
  if (['error', 'warning'].includes(type)) {
    report.console.push({ type, text: msg.text() });
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
  await screenshot(page, '01-login');

  await page.getByPlaceholder(/账号|用户名|请输入账号|请输入用户名/).fill('admin');
  await page.getByPlaceholder(/密码|请输入密码/).fill('admin123');
  await page.getByRole('button', { name: /登\s*录/ }).click();
  await page.waitForURL(/#\/(home|dashboard|procurement|system|rbac)/, { timeout: 15000 });
  await page.waitForLoadState('networkidle');
  await screenshot(page, '02-home-after-login');

  const homeText = await page.locator('body').innerText();
  check('login-home', !homeText.includes('404') && homeText.trim().length > 0, {
    url: page.url(),
    textSample: homeText.trim().slice(0, 200),
  });

  await page.goto(`${report.baseUrl}/#/procurement`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  await screenshot(page, '03-procurement-parent-redirect');
  const procurementUrl = page.url();
  const procurementText = await page.locator('body').innerText();
  check('procurement-parent-redirect', procurementUrl.includes('/procurement/orders') && !procurementText.includes('404'), {
    url: procurementUrl,
    hasOrderText: procurementText.includes('Order名称') || procurementText.includes('采购管理'),
    textSample: procurementText.trim().slice(0, 200),
  });

  const orderName = `验收采购订单-${Date.now()}`;
  const searchInput = page.locator('.procurement-order-page input:not([type="hidden"])').first();
  await searchInput.fill(orderName);
  await page.getByRole('button', { name: /新增|新建|创建/ }).first().click();
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1000);

  const afterCreateText = await page.locator('body').innerText();
  check('order-create-visible', afterCreateText.includes(orderName), { orderName });

  const dbCount = mysqlScalar(`SELECT COUNT(*) FROM procurement_order WHERE name='${orderName.replaceAll("'", "''")}'`);
  check('order-create-persisted', Number(dbCount) >= 1, { orderName, dbCount });
  await screenshot(page, '04-order-after-create');

  await searchInput.fill(orderName);
  await page.getByRole('button', { name: /查询|搜索/ }).first().click();
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(800);
  const afterSearchText = await page.locator('body').innerText();
  check('order-search-visible', afterSearchText.includes(orderName), { orderName });
  await screenshot(page, '05-order-after-search');

  check('no-network-failures', report.failedRequests.length === 0, {
    failedRequests: report.failedRequests,
  });
} finally {
  await fs.writeFile(path.join(evidenceDir, 'browser-report.json'), JSON.stringify(report, null, 2));
  await browser.close();
}
