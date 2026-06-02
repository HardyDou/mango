import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const evidenceDir = dirname(fileURLToPath(import.meta.url));
const require = createRequire(resolve(evidenceDir, '../../../mango-ui/apps/mango-admin/package.json'));
const { chromium } = require('@playwright/test');
const screenshotDir = resolve(evidenceDir, 'screenshots');
mkdirSync(screenshotDir, { recursive: true });

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:8512';
const report = {
  baseURL,
  startedAt: new Date().toISOString(),
  pages: [],
  console: [],
  network: [],
};

const browser = await chromium.launch({ headless: true });
const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });

page.on('console', (message) => {
  const type = message.type();
  if (['error', 'warning'].includes(type)) {
    report.console.push({
      type,
      text: message.text(),
      location: message.location(),
    });
  }
});

page.on('response', (response) => {
  const status = response.status();
  const url = response.url();
  if (status >= 400 || /\.(js|css|vue|png|jpg|jpeg|svg|woff2?)($|\?)/.test(url) && status === 404) {
    report.network.push({ status, url });
  }
});

try {
  await login(page);
  await verifyPage(page, {
    id: 'ER-003',
    name: 'upload-components',
    path: '/#/components/upload',
    assertions: ['MUpload 文件上传', '通用/默认用法', '上传文件'],
  });
  await verifyPage(page, {
    id: 'ER-004',
    name: 'workflow-components',
    path: '/#/components/workflow',
    assertions: ['工作流组件', '运行时表单渲染', '业务申请组件注册'],
  });
  await verifyPage(page, {
    id: 'ER-007',
    name: 'system-user',
    path: '/#/system/user',
    assertions: ['成员管理', '用户名', '查询'],
  });
  await verifyWorkflowDesigner(page);
} finally {
  report.finishedAt = new Date().toISOString();
  writeFileSync(resolve(evidenceDir, 'monolith-readiness-report.json'), `${JSON.stringify(report, null, 2)}\n`);
  await browser.close();
}

const blockingConsole = report.console.filter((item) =>
  /Vue 错误|Cannot read properties|X-Frame-Options|Content Security Policy|Invalid prop|deprecated/i.test(item.text)
);
const blockingNetwork = report.network.filter((item) => item.status === 404 || item.status >= 500);
const failedPages = report.pages.filter((item) => item.result !== 'PASS');

if (failedPages.length || blockingConsole.length || blockingNetwork.length) {
  console.error(JSON.stringify({ failedPages, blockingConsole, blockingNetwork }, null, 2));
  process.exit(1);
}

async function login(page) {
  await page.goto(`${baseURL}/#/login`, { waitUntil: 'networkidle' });
  await page.locator('input[placeholder="用户名"]').fill('admin');
  await page.locator('input[placeholder="密码"]').fill('admin123');
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.locator('button:has-text("登 录")').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function verifyPage(page, target) {
  const beforeConsole = report.console.length;
  const beforeNetwork = report.network.length;
  await page.goto(`${baseURL}${target.path}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);

  const bodyText = await page.locator('body').innerText();
  const missing = target.assertions.filter((text) => !bodyText.includes(text));
  const screenshot = resolve(screenshotDir, `${target.name}.png`);
  await page.screenshot({ path: screenshot, fullPage: true });
  report.pages.push({
    id: target.id,
    name: target.name,
    path: target.path,
    assertions: target.assertions,
    missing,
    consoleCount: report.console.length - beforeConsole,
    networkCount: report.network.length - beforeNetwork,
    screenshot,
    result: missing.length ? 'FAIL' : 'PASS',
  });
}

async function verifyWorkflowDesigner(page) {
  const target = {
    id: 'ER-005',
    name: 'workflow-designer-drag',
    path: '/#/workflow/manage/definition',
  };
  const beforeConsole = report.console.length;
  const beforeNetwork = report.network.length;
  await page.goto(`${baseURL}${target.path}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);

  const createButton = page.getByRole('button', { name: /新增流程|创建流程/ }).first();
  if (!(await createButton.isVisible().catch(() => false))) {
    await page.screenshot({ path: resolve(screenshotDir, `${target.name}-missing-create-button.png`), fullPage: true });
  }
  await createButton.click();

  const unique = Date.now();
  await page.locator('.builder-form .el-select').first().click();
  await page.getByRole('option').first().click();
  await page.getByPlaceholder('请输入流程名称').fill(`推广验收流程${unique}`);
  await page.getByPlaceholder('如 contract_approve').fill(`enterprise_acceptance_${unique}`);
  await page.getByRole('button', { name: '下一步' }).click();
  await page.getByPlaceholder('如 contract_apply_form').fill(`enterprise_form_${unique}`);
  await page.getByRole('radio', { name: '内置设计器' }).check();
  await page.waitForSelector('.workflow-form-designer', { timeout: 10000 });

  const designer = page.locator('.workflow-form-designer');
  const firstComponent = designer.getByText('输入框', { exact: true }).first();
  const canvas = designer.locator('._fc-m-drag, .fc-designer-drag, ._fc-m').first();
  await firstComponent.dragTo(canvas);
  await page.waitForTimeout(1000);

  const bodyText = await page.locator('body').innerText();
  const screenshot = resolve(screenshotDir, `${target.name}.png`);
  await page.screenshot({ path: screenshot, fullPage: true });
  const hasInput = bodyText.includes('输入框') && await designer.locator('input, .el-input').count() > 0;
  report.pages.push({
    id: target.id,
    name: target.name,
    path: target.path,
    assertions: ['打开流程定义新增流程', '进入表单信息', '拖入输入框组件'],
    missing: hasInput ? [] : ['拖入输入框组件后画布未出现输入控件'],
    consoleCount: report.console.length - beforeConsole,
    networkCount: report.network.length - beforeNetwork,
    screenshot,
    result: hasInput ? 'PASS' : 'FAIL',
  });
}
