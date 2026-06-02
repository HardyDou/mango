import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const evidenceDir = dirname(fileURLToPath(import.meta.url));
const require = createRequire(resolve(evidenceDir, '../../../mango-ui/apps/mango-admin-shell/package.json'));
const { chromium } = require('@playwright/test');
const screenshotDir = resolve(evidenceDir, 'screenshots');
mkdirSync(screenshotDir, { recursive: true });

const baseURL = process.env.PLAYWRIGHT_BASE_URL || 'http://a.mango.io:5176';
const runtimeConfigPath = resolve(evidenceDir, '../../../mango-ui/apps/mango-admin-shell/public/runtime-config.json');
const originalRuntimeConfig = existsSync(runtimeConfigPath) ? readFileSync(runtimeConfigPath, 'utf8') : '';
const hybridConfig = {
  profile: 'hybrid',
  modules: {
    'mango-authorization': {
      mode: 'micro',
      runtimeCode: 'mango-admin-rbac-app',
      entry: 'http://b.mango.io:5181/',
    },
    'mango-system': {
      mode: 'local',
      runtimeCode: 'mango-admin-system-local',
    },
    'mango-workflow': {
      mode: 'micro',
      runtimeCode: 'mango-admin-workflow-app',
      entry: 'http://c.mango.io:5182/',
    },
  },
};

const report = {
  baseURL,
  startedAt: new Date().toISOString(),
  pages: [],
  console: [],
  network: [],
};

writeFileSync(runtimeConfigPath, `${JSON.stringify(hybridConfig, null, 2)}\n`);

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
  await verifyShellLocalPage(page, {
    id: 'ER-014',
    name: 'micro-upload-components',
    path: '/#/components/upload',
    assertions: ['MUpload 文件上传', '通用/默认用法', '上传文件'],
  });
  await verifyShellLocalPage(page, {
    id: 'ER-014',
    name: 'micro-workflow-components',
    path: '/#/components/workflow',
    assertions: ['工作流组件', '运行时表单渲染', '业务申请组件注册'],
  });
  await verifyRemotePage(page, {
    id: 'ER-014',
    name: 'micro-system-role',
    path: '/#/system/role',
    runtime: {
      moduleCode: 'mango-authorization',
      runtimeCode: 'mango-admin-rbac-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: 'b.mango.io:5181',
    },
    assertions: ['新增角色', '角色编码', '分配权限'],
  });
  await verifyRemoteWorkflowDesigner(page);
} finally {
  report.finishedAt = new Date().toISOString();
  writeFileSync(resolve(evidenceDir, 'micro-readiness-report.json'), `${JSON.stringify(report, null, 2)}\n`);
  await browser.close();
  if (originalRuntimeConfig) {
    writeFileSync(runtimeConfigPath, originalRuntimeConfig);
  }
}

const blockingConsole = report.console.filter((item) =>
  /Vue 错误|Cannot read properties|X-Frame-Options|Content Security Policy|Invalid prop|deprecated|Cannot find module|页面加载失败/i.test(item.text)
  && !/Invalid event arguments: event validation failed/i.test(item.text)
  && !/WebSocket connection to .*realtime\/transports\/probe\/websocket/i.test(item.text)
);
const blockingNetwork = report.network.filter((item) => item.status === 404 || item.status >= 500);
const failedPages = report.pages.filter((item) => item.result !== 'PASS');

if (failedPages.length || blockingConsole.length || blockingNetwork.length) {
  console.error(JSON.stringify({ failedPages, blockingConsole, blockingNetwork }, null, 2));
  process.exit(1);
}

async function login(page) {
  await page.goto(`${baseURL}/#/login`, { waitUntil: 'networkidle' });
  await page.getByPlaceholder('用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200,
    { timeout: 10000 },
  ).catch(() => null);
  await page.getByPlaceholder('密码').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.getByRole('button', { name: /登\s*录/ }).click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
  await page.locator('.shell-runtime-content').waitFor({ timeout: 10000 });
}

async function verifyShellLocalPage(page, target) {
  const beforeConsole = report.console.length;
  const beforeNetwork = report.network.length;
  await page.goto(`${baseURL}${target.path}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  const bodyText = await pageText(page);
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

async function verifyRemotePage(page, target) {
  const beforeConsole = report.console.length;
  const beforeNetwork = report.network.length;
  await page.goto(`${baseURL}${target.path}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  const runtime = await currentRuntime(page);
  const bodyText = await pageText(page);
  const missing = target.assertions.filter((text) => !bodyText.includes(text));
  const runtimeMismatch = [
    runtime.moduleCode !== target.runtime.moduleCode ? `moduleCode=${runtime.moduleCode}` : '',
    runtime.runtimeCode !== target.runtime.runtimeCode ? `runtimeCode=${runtime.runtimeCode}` : '',
    runtime.pageType !== target.runtime.pageType ? `pageType=${runtime.pageType}` : '',
    !runtime.entry.includes(target.runtime.entryIncludes) ? `entry=${runtime.entry}` : '',
  ].filter(Boolean);
  const screenshot = resolve(screenshotDir, `${target.name}.png`);
  await page.screenshot({ path: screenshot, fullPage: true });
  report.pages.push({
    id: target.id,
    name: target.name,
    path: target.path,
    runtime,
    assertions: target.assertions,
    missing: [...missing, ...runtimeMismatch],
    consoleCount: report.console.length - beforeConsole,
    networkCount: report.network.length - beforeNetwork,
    screenshot,
    result: missing.length || runtimeMismatch.length ? 'FAIL' : 'PASS',
  });
}

async function verifyRemoteWorkflowDesigner(page) {
  const target = {
    id: 'ER-014',
    name: 'micro-workflow-designer-drag',
    path: '/#/workflow/manage/definition',
    runtime: {
      moduleCode: 'mango-workflow',
      runtimeCode: 'mango-admin-workflow-app',
      pageType: 'MICRO_ROUTE',
      entryIncludes: 'c.mango.io:5182',
    },
  };
  const beforeConsole = report.console.length;
  const beforeNetwork = report.network.length;
  await page.goto(`${baseURL}${target.path}`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  const runtime = await currentRuntime(page);

  const createButton = page.getByRole('button', { name: /新增流程|创建流程/ }).first();
  if (!(await createButton.isVisible().catch(() => false))) {
    await page.screenshot({ path: resolve(screenshotDir, `${target.name}-missing-create-button.png`), fullPage: true });
  }
  await createButton.click();
  const unique = Date.now();
  await page.locator('.builder-form .el-select').first().click();
  await page.getByRole('option').first().click();
  const nameInput = page.getByPlaceholder('请输入流程名称');
  const keyInput = page.getByPlaceholder(/如 .*_approve/);
  if (!(await keyInput.isVisible().catch(() => false))) {
    await page.screenshot({ path: resolve(screenshotDir, `${target.name}-missing-key-input.png`), fullPage: true });
  }
  await nameInput.fill(`微前端验收流程${unique}`);
  await keyInput.fill(`micro_acceptance_${unique}`);
  await page.getByRole('button', { name: '下一步' }).click();
  await page.waitForTimeout(800);
  const formCodeInput = page.locator('input[placeholder*="_apply_form"]').first();
  if (!(await formCodeInput.isVisible().catch(() => false))) {
    const diagnosticScreenshot = resolve(screenshotDir, `${target.name}-missing-form-code-input.png`);
    await page.screenshot({ path: diagnosticScreenshot, fullPage: true });
    report.pages.push({
      id: target.id,
      name: `${target.name}-diagnostic`,
      path: target.path,
      runtime,
      assertions: ['进入表单信息后出现表单编码输入框'],
      missing: await collectInputDiagnostics(page),
      consoleCount: report.console.length - beforeConsole,
      networkCount: report.network.length - beforeNetwork,
      screenshot: diagnosticScreenshot,
      result: 'FAIL',
    });
    return;
  }
  await formCodeInput.fill(`micro_form_${unique}`);
  await page.getByRole('radio', { name: '内置设计器' }).check();
  await page.waitForSelector('.workflow-form-designer', { timeout: 10000 });

  const designer = page.locator('.workflow-form-designer');
  const firstComponent = designer.getByText('输入框', { exact: true }).first();
  const canvas = designer.locator('._fc-m-drag, .fc-designer-drag, ._fc-m').first();
  await firstComponent.dragTo(canvas);
  await page.waitForTimeout(1000);

  const bodyText = await pageText(page);
  const hasInput = bodyText.includes('输入框') && await designer.locator('input, .el-input').count() > 0;
  const runtimeMismatch = [
    runtime.moduleCode !== target.runtime.moduleCode ? `moduleCode=${runtime.moduleCode}` : '',
    runtime.runtimeCode !== target.runtime.runtimeCode ? `runtimeCode=${runtime.runtimeCode}` : '',
    runtime.pageType !== target.runtime.pageType ? `pageType=${runtime.pageType}` : '',
    !runtime.entry.includes(target.runtime.entryIncludes) ? `entry=${runtime.entry}` : '',
  ].filter(Boolean);
  const screenshot = resolve(screenshotDir, `${target.name}.png`);
  await page.screenshot({ path: screenshot, fullPage: true });
  report.pages.push({
    id: target.id,
    name: target.name,
    path: target.path,
    runtime,
    assertions: ['远程 workflow 子应用承载', '进入表单信息', '拖入输入框组件'],
    missing: hasInput ? runtimeMismatch : ['拖入输入框组件后画布未出现输入控件', ...runtimeMismatch],
    consoleCount: report.console.length - beforeConsole,
    networkCount: report.network.length - beforeNetwork,
    screenshot,
    result: hasInput && runtimeMismatch.length === 0 ? 'PASS' : 'FAIL',
  });
}

async function currentRuntime(page) {
  return page.locator('.shell-runtime-content').evaluate((el) => ({
    moduleCode: el.dataset.mangoRuntimeModule || '',
    runtimeCode: el.dataset.mangoRuntimeCode || '',
    pageType: el.dataset.mangoRuntimePageType || '',
    entry: el.dataset.mangoRuntimeEntry || '',
  }));
}

async function pageText(page) {
  const texts = await page.locator('body').allInnerTexts();
  return texts.join('\n');
}

async function collectInputDiagnostics(page) {
  return page.evaluate(() =>
    Array.from(document.querySelectorAll('input, textarea'))
      .map((input) => {
        const element = input;
        return `${element.tagName.toLowerCase()} placeholder="${element.getAttribute('placeholder') || ''}" value="${element.value || ''}" visible="${Boolean(element.offsetParent)}"`;
      })
      .slice(0, 30)
  );
}
