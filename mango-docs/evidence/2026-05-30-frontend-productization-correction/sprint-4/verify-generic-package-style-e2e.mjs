import { createRequire } from 'node:module';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';

const repoRoot = process.cwd();
const require = createRequire(join(repoRoot, 'mango-ui/node_modules/.pnpm/node_modules/playwright/package.json'));
const { chromium } = require('playwright');
const baseURL = process.env.MANGO_E2E_BASE_URL || 'http://a.mango.io:5176';
const evidenceDir = join(repoRoot, 'mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-4/regression-2026-05-31');
const screenshotDir = join(evidenceDir, 'screenshots');
const reportPath = join(evidenceDir, 'generic-package-style-generator-e2e-final.json');
mkdirSync(screenshotDir, { recursive: true });

const runId = Date.now();
const consoleErrors = [];
const pageErrors = [];
const networkFailures = [];
const badResponses = [];
const checks = [];
const screenshots = [];

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  baseURL,
  viewport: { width: 1440, height: 960 },
});
const page = await context.newPage();

page.on('console', (message) => {
  if (message.type() === 'error') {
    const text = message.text();
    if (!text.includes('ResizeObserver loop')) {
      consoleErrors.push(text);
    }
  }
});
page.on('pageerror', (error) => {
  pageErrors.push(error.message);
});
page.on('requestfailed', (request) => {
  const url = request.url();
  if (!url.includes('/sockjs-node') && !url.includes('/__vite_ping')) {
    networkFailures.push({ url, failure: request.failure()?.errorText || '' });
  }
});
page.on('response', (response) => {
  const url = response.url();
  const status = response.status();
  if (status >= 400 && !url.includes('/favicon') && !url.includes('/notifications/unread-count')) {
    badResponses.push({ url, status });
  }
});

try {
  await login();
  await captureHome();
  await verifyPage({
    name: 'workflow-start',
    path: '/#/workflow/start-process',
    screenshotName: 'generic-style-final-workflow-start.png',
    waitText: '发起流程',
    selectors: {
      '.workflow-launch-card': [
        ['display', 'grid'],
        ['padding', '14px'],
        ['borderRadius', '8px'],
      ],
    },
  });
  await verifyPage({
    name: 'template-templates',
    path: '/#/template/templates',
    screenshotName: 'generic-style-final-template-templates.png',
    waitText: '新增模板',
    selectors: {
      '.template-container': [['display', 'flex']],
      '.template-main': [['display', 'flex']],
    },
  });
  await verifyPage({
    name: 'system-menu-package',
    path: '/#/system/menu-package',
    screenshotName: 'generic-style-final-system-menu-package.png',
    waitText: '新增套餐',
    selectors: {
      '.menu-package-container': [['display', 'block']],
      '.search-form': [['marginBottom', '16px']],
    },
  });
  await verifyPage({
    name: 'dev-upload',
    path: '/#/components/upload',
    screenshotName: 'generic-style-final-dev-upload.png',
    waitText: 'Upload 文件上传',
    selectors: {
      '.mango-file-upload': [
        ['display', 'inline-block'],
        ['widthCssNot', 'auto'],
      ],
      '.upload-control': [
        ['display', 'flex'],
        ['gap', '12px'],
      ],
    },
  });
} finally {
  await browser.close();
}

const report = {
  pass: checks.every((item) => item.pass) && consoleErrors.length === 0 && pageErrors.length === 0 && networkFailures.length === 0 && badResponses.length === 0,
  checkedAt: new Date().toISOString(),
  baseURL,
  checks,
  consoleErrors,
  pageErrors,
  networkFailures,
  badResponses,
  screenshots,
};
writeFileSync(reportPath, JSON.stringify(report, null, 2));

if (!report.pass) {
  console.error(`Generic package style E2E failed. Report: ${reportPath}`);
  process.exit(1);
}

console.log(`Generic package style E2E passed. Report: ${reportPath}`);

async function login() {
  await page.goto('/#/login', { waitUntil: 'domcontentloaded' });
  await page.locator('input[placeholder="用户名"]').fill('admin');
  await page.locator('input[placeholder="密码"]').fill('admin123');
  const tenantRequest = page
    .waitForResponse((response) => response.url().includes('/api/auth/login-institutions') && response.status() === 200, {
      timeout: 10000,
    })
    .catch(() => undefined);
  await page.locator('input[placeholder="密码"]').blur();
  await page.locator('.tenant-select').click();
  await tenantRequest;
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 15000 });
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined);
}

async function captureHome() {
  await page.goto(`/#/home?genericStyleProbe=${runId}`);
  await page.waitForURL('**/#/home**', { timeout: 10000 });
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined);
  await assertShell('home');
  await screenshot('generic-style-final-home.png');
}

async function verifyPage(options) {
  await page.goto(`${options.path}?genericStyleProbe=${runId}`);
  await page.waitForURL(`**${options.path}**`, { timeout: 10000 });
  await page.getByText(options.waitText, { exact: false }).first().waitFor({ state: 'visible', timeout: 15000 });
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined);
  const shell = await assertShell(options.name);
  const elements = {};
  const styleResults = [];

  for (const [selector, assertions] of Object.entries(options.selectors)) {
    const locator = page.locator(selector).first();
    await locator.waitFor({ state: 'visible', timeout: 10000 });
    elements[selector] = await locator.evaluate((element) => {
      const style = window.getComputedStyle(element);
      const rect = element.getBoundingClientRect();
      return {
        className: element.className,
        text: element.textContent?.replace(/\s+/g, ' ').trim().slice(0, 180),
        display: style.display,
        widthCss: style.width,
        padding: style.padding,
        marginBottom: style.marginBottom,
        borderRadius: style.borderRadius,
        backgroundColor: style.backgroundColor,
        justifyContent: style.justifyContent,
        alignItems: style.alignItems,
        gap: style.gap,
        width: rect.width,
        height: rect.height,
      };
    });

    for (const [prop, expected] of assertions) {
      const actual = elements[selector][prop === 'widthCssNot' ? 'widthCss' : prop];
      const pass = prop === 'widthCssNot' ? actual !== expected : actual === expected;
      styleResults.push({ selector, prop, expected, actual, pass });
    }
  }

  const bodyText = await page.evaluate(() => document.body.innerText);
  const has404OrError = /404|页面加载失败|Page Not Found|Cannot GET|加载失败/.test(bodyText);
  const screenshotPath = await screenshot(options.screenshotName);
  const pass = styleResults.every((item) => item.pass) && !has404OrError && shell.topbarVisible && shell.asideVisible && shell.tagsVisible && shell.noticeBellVisible;

  checks.push({
    name: options.name,
    url: page.url(),
    screenshot: screenshotPath,
    shell,
    elements,
    styleResults,
    has404OrError,
    pass,
  });
}

async function assertShell(name) {
  const shell = {
    topbarVisible: await page.locator('.admin-header, .layout-header, header').first().isVisible().catch(() => false),
    asideVisible: await page.locator('.admin-aside, .layout-aside, aside, .el-menu').first().isVisible().catch(() => false),
    tagsVisible: await page.locator('.tags-view, .layout-tags, .tags-view-container').first().isVisible().catch(() => false),
    noticeBellVisible: await page.locator('.mango-notice-trigger, [class*="notice"]').first().isVisible().catch(() => false),
    noticeTrigger: await page.locator('.mango-notice-trigger, [class*="notice"]').first().evaluate((element) => {
      const style = window.getComputedStyle(element);
      const rect = element.getBoundingClientRect();
      return {
        display: style.display,
        backgroundColor: style.backgroundColor,
        borderRadius: style.borderRadius,
        border: style.border,
        color: style.color,
        width: rect.width,
        height: rect.height,
      };
    }).catch(() => null),
  };
  if (!shell.topbarVisible || !shell.asideVisible || !shell.tagsVisible || !shell.noticeBellVisible) {
    checks.push({ name: `${name}-shell`, shell, pass: false });
  }
  return shell;
}

async function screenshot(name) {
  const path = resolve(screenshotDir, name);
  await page.screenshot({ path, fullPage: true });
  screenshots.push(path);
  return path;
}
