import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const evidenceDir = dirname(fileURLToPath(import.meta.url));
const projectRoot = '/tmp/mango-enterprise-cli-1.0.17-172024/procurement-platform';
const require = createRequire(resolve(projectRoot, 'frontend/package.json'));
const { chromium } = require('playwright');

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:19081';
const screenshotDir = resolve(evidenceDir, 'screenshots');
mkdirSync(screenshotDir, { recursive: true });

const report = {
  baseURL,
  projectRoot,
  startedAt: new Date().toISOString(),
  checks: [],
  console: [],
  network: [],
};

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });

page.on('console', message => {
  if (['error', 'warning'].includes(message.type())) {
    report.console.push({
      type: message.type(),
      text: message.text(),
      location: message.location(),
    });
  }
});

page.on('response', response => {
  const status = response.status();
  const url = response.url();
  if (status >= 400 || (status === 404 && /\.(js|css|png|jpg|jpeg|svg|woff2?)($|\?)/.test(url))) {
    report.network.push({ status, url });
  }
});

try {
  await login();
  await verifyHomeShell();
  await verifyProcurementParentRedirect();
  await verifyProcurementCrud();
  await verifyTagCloseFallback();
} finally {
  report.finishedAt = new Date().toISOString();
  writeFileSync(resolve(evidenceDir, 'browser-report.json'), `${JSON.stringify(report, null, 2)}\n`);
  await browser.close();
}

const failed = report.checks.filter(check => check.result !== 'PASS');
const blockingConsole = report.console.filter(item =>
  /Vue 错误|Cannot read properties|Failed to fetch dynamically imported module|deprecated usage|Invalid prop|Content Security Policy|X-Frame-Options/i.test(item.text)
);
const blockingNetwork = report.network.filter(item => item.status === 404 || item.status >= 500);

if (failed.length || blockingConsole.length || blockingNetwork.length) {
  console.error(JSON.stringify({ failed, blockingConsole, blockingNetwork }, null, 2));
  process.exit(1);
}

async function login() {
  await page.goto(`${baseURL}/#/login`, { waitUntil: 'networkidle' });
  await page.locator('input[placeholder="用户名"]').fill('admin');
  await page.locator('input[placeholder="密码"]').fill('admin123');
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.locator('button:has-text("登 录")').click();
  await page.waitForURL('**/#/home', { timeout: 15000 });
  await page.waitForLoadState('networkidle');
  await saveCheck('login-home', ['首页', '芒果集团'], async () => page.locator('body').innerText());
}

async function verifyHomeShell() {
  await page.screenshot({ path: resolve(screenshotDir, '01-home-shell.png'), fullPage: true });
  const text = await page.locator('body').innerText();
  const noticeIconCount = await page.locator('.el-icon, svg').evaluateAll(nodes =>
    nodes.filter(node => /bell|notification|notice/i.test(`${node.getAttribute('class') || ''} ${node.outerHTML}`)).length
  );
  report.checks.push({
    name: 'home-shell',
    assertions: ['home is not 404', 'tenant name visible', 'notice entry visible'],
    details: { noticeIconCount },
    screenshot: resolve(screenshotDir, '01-home-shell.png'),
    result: !text.includes('404') && text.includes('芒果集团') && noticeIconCount > 0 ? 'PASS' : 'FAIL',
  });
}

async function verifyProcurementParentRedirect() {
  await page.goto(`${baseURL}/#/procurement`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  await page.screenshot({ path: resolve(screenshotDir, '02-procurement-parent-redirect.png'), fullPage: true });
  const text = await page.locator('body').innerText();
  report.checks.push({
    name: 'procurement-parent-redirect',
    assertions: ['parent menu redirects to child page', 'no 404', 'business table visible'],
    url: page.url(),
    screenshot: resolve(screenshotDir, '02-procurement-parent-redirect.png'),
    result: page.url().includes('/procurement/orders')
      && text.includes('Order名称')
      && text.includes('新增')
      && !text.includes('404')
      ? 'PASS'
      : 'FAIL',
  });
}

async function verifyProcurementCrud() {
  const recordName = `采购验收订单-${Date.now()}`;
  await page.goto(`${baseURL}/#/procurement/orders`, { waitUntil: 'networkidle' });
  await page.locator('input[placeholder="Order名称"]').fill(recordName);
  await page.getByRole('button', { name: '新增' }).click();
  await page.waitForResponse(response =>
    response.url().includes('/api/procurement/orders/create') && response.status() < 400,
    { timeout: 10000 }
  );
  await page.waitForResponse(response =>
    response.url().includes('/api/procurement/orders/page') && response.status() < 400,
    { timeout: 10000 }
  );
  await page.waitForTimeout(500);
  await page.screenshot({ path: resolve(screenshotDir, '03-procurement-order-after-create.png'), fullPage: true });
  const textAfterCreate = await page.locator('body').innerText();

  await page.locator('input[placeholder="Order名称"]').fill(recordName);
  await page.getByRole('button', { name: '查询' }).click();
  await page.waitForResponse(response =>
    response.url().includes('/api/procurement/orders/page') && response.status() < 400,
    { timeout: 10000 }
  );
  await page.waitForTimeout(500);
  await page.screenshot({ path: resolve(screenshotDir, '04-procurement-order-after-search.png'), fullPage: true });
  const textAfterSearch = await page.locator('body').innerText();

  report.checks.push({
    name: 'procurement-order-crud',
    assertions: ['create request succeeds', 'created row appears', 'query filters and keeps row visible'],
    testData: { recordName },
    screenshots: [
      resolve(screenshotDir, '03-procurement-order-after-create.png'),
      resolve(screenshotDir, '04-procurement-order-after-search.png'),
    ],
    result: textAfterCreate.includes(recordName) && textAfterSearch.includes(recordName) ? 'PASS' : 'FAIL',
  });
}

async function verifyTagCloseFallback() {
  await page.goto(`${baseURL}/#/system/user`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  await page.goto(`${baseURL}/#/procurement/orders`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);

  const closeButtons = page.locator('.tags-view, .tags-view-wrapper, .tag-view').locator('.el-icon-close, .close, [class*="close"]');
  const count = await closeButtons.count();
  if (count > 0) {
    await closeButtons.nth(count - 1).click();
    await page.waitForTimeout(800);
  }
  await page.screenshot({ path: resolve(screenshotDir, '05-tag-close-fallback.png'), fullPage: true });
  const text = await page.locator('body').innerText();
  report.checks.push({
    name: 'tag-close-fallback-smoke',
    assertions: ['closing a tag does not leave blank shell', 'no system error text'],
    closeButtonCount: count,
    url: page.url(),
    screenshot: resolve(screenshotDir, '05-tag-close-fallback.png'),
    result: count > 0 && !text.includes('系统错误') && !text.includes('404') && text.trim().length > 0 ? 'PASS' : 'FAIL',
  });
}

async function saveCheck(name, assertions, readText) {
  const text = await readText();
  report.checks.push({
    name,
    assertions,
    result: assertions.every(assertion => text.includes(assertion)) ? 'PASS' : 'FAIL',
  });
}
