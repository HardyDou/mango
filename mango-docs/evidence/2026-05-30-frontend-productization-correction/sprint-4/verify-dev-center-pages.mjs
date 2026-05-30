import fs from 'node:fs/promises';
import path from 'node:path';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(__dirname, '../../../..');
const require = createRequire(path.join(repoRoot, 'mango-ui/apps/mango-admin-shell/package.json'));
const { chromium } = require('@playwright/test');

const frontendUrl = process.env.MANGO_MICRO_FRONTEND_URL || 'http://a.mango.io:5176';
const screenshotDir = path.join(__dirname, 'screenshots');
const reportPath = path.join(__dirname, 'dev-center-pages-report.json');

const credentials = {
  username: 'admin',
  password: 'admin123',
};

const targetPages = [
  { name: '文件上传', path: '/components/upload' },
  { name: '组织架构选择器', path: '/components/org-selector' },
  { name: '省市区选择器', path: '/components/china-area' },
  { name: 'AI 对话', path: '/components/chat' },
  { name: '实时通信', path: '/components/realtime' },
  { name: '验证码', path: '/components/captcha' },
];

const state = {
  frontendUrl,
  startedAt: new Date().toISOString(),
  screenshots: [],
  pageErrors: [],
  consoleErrors: [],
  networkFailures: [],
  ignoredNetworkFailures: [],
  failedResponses: [],
  pages: [],
};

function sanitizeFileName(value) {
  return value.replace(/[\\/:*?"<>|\s]+/g, '-');
}

function isIgnoredNetworkFailure(request) {
  const failureText = request.failure()?.errorText || '';
  return request.url().includes('/api/realtime/transports/probe/sse')
    && failureText.includes('ERR_ABORTED');
}

async function screenshot(page, fileName, options = {}) {
  const fullPath = path.join(screenshotDir, fileName);
  await page.screenshot({ path: fullPath, fullPage: options.fullPage ?? true });
  state.screenshots.push(fullPath);
  return fullPath;
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
}

async function collectEvidence(page, targetPage) {
  const shell = {
    topbarVisible: await page.locator('.layout-navbars-container').first().isVisible().catch(() => false),
    asideVisible: await page.locator('.layout-aside').first().isVisible().catch(() => false),
    tagsVisible: await page.locator('.tags-view-container').first().isVisible().catch(() => false),
    noticeBellVisible: await page.locator('.notice-bell').first().isVisible().catch(() => false),
    settingsVisible: await page.locator('.layout-breadcrumb-settings').first().isVisible().catch(() => false),
    userVisible: await page.locator('.layout-breadcrumb-user').first().isVisible().catch(() => false),
  };
  const runtime = await page.locator('.shell-runtime-content').evaluate((el) => ({
    moduleCode: el.dataset.mangoRuntimeModule || '',
    runtimeCode: el.dataset.mangoRuntimeCode || '',
    pageType: el.dataset.mangoRuntimePageType || '',
  })).catch(() => ({}));
  const errorText = page.locator('.shell-runtime-content').getByText(
    /^(401|403|404|未授权|拒绝访问|路由加载失败|页面加载失败|加载失败)$/
  ).first();
  return {
    name: targetPage.name,
    path: targetPage.path,
    url: page.url(),
    titleVisible: await page.getByText(targetPage.name).first().isVisible().catch(() => false),
    has404OrError: await errorText.isVisible().catch(() => false),
    shell,
    runtime,
    bodyTextSample: (await page.locator('main').innerText().catch(() => '')).slice(0, 500),
  };
}

async function main() {
  await fs.mkdir(screenshotDir, { recursive: true });
  const browser = await chromium.launch({ headless: true });
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

  try {
    await loginUi(page);
    await screenshot(page, 'dev-center-home-1440x960.png', { fullPage: false });
    for (const targetPage of targetPages) {
      await page.goto(`${frontendUrl}/#${targetPage.path}`, { waitUntil: 'domcontentloaded' });
      await page.waitForURL(`**/#${targetPage.path}`, { timeout: 15000 });
      await page.locator('.shell-runtime-content').waitFor({ state: 'visible', timeout: 15000 });
      await page.waitForTimeout(1200);
      const shot = await screenshot(page, `s4-dev-center-${sanitizeFileName(targetPage.name)}.png`);
      state.pages.push({
        screenshot: shot,
        evidence: await collectEvidence(page, targetPage),
      });
    }
  } finally {
    await browser.close();
  }

  state.finishedAt = new Date().toISOString();
  await fs.writeFile(reportPath, `${JSON.stringify(state, null, 2)}\n`);

  const pageFailures = state.pages.filter((item) =>
    !item.evidence.titleVisible
    || item.evidence.has404OrError
    || !item.evidence.shell.topbarVisible
    || !item.evidence.shell.asideVisible
    || !item.evidence.shell.noticeBellVisible
    || !item.evidence.shell.userVisible
  );

  if (state.pageErrors.length || state.consoleErrors.length || state.networkFailures.length || state.failedResponses.length || pageFailures.length) {
    throw new Error(JSON.stringify({
      reportPath,
      pageErrors: state.pageErrors,
      consoleErrors: state.consoleErrors,
      networkFailures: state.networkFailures,
      failedResponses: state.failedResponses,
      pageFailures,
    }, null, 2));
  }

  console.log(JSON.stringify({
    reportPath,
    screenshots: state.screenshots,
    pages: state.pages.map(item => ({
      name: item.evidence.name,
      path: item.evidence.path,
      titleVisible: item.evidence.titleVisible,
      shell: item.evidence.shell,
      screenshot: item.screenshot,
    })),
    pageErrors: state.pageErrors,
    consoleErrors: state.consoleErrors,
    networkFailures: state.networkFailures,
    ignoredNetworkFailures: state.ignoredNetworkFailures,
    failedResponses: state.failedResponses,
  }, null, 2));
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
