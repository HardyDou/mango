import { createRequire } from 'node:module';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(scriptDir, '../../../..');
const shellDir = resolve(repoRoot, 'mango-ui/apps/mango-admin-shell');
const requireFromShell = createRequire(resolve(shellDir, 'package.json'));
const { chromium } = requireFromShell('@playwright/test');

const baseUrl = process.env.PLAYWRIGHT_BASE_URL || 'http://a.mango.io:5176';
const apiBaseUrl = process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:5555';
const runtimeConfigPath = resolve(shellDir, 'public/runtime-config.json');
const screenshotDir = resolve(scriptDir, 'screenshots');
const reportDir = resolve(scriptDir, 'reports');
const reportPath = resolve(reportDir, 'mode-matrix-report.json');

const commonModules = {
  'mango-system': {
    mode: 'local',
    runtimeCode: 'mango-admin-system-local',
  },
};

const modes = [
  {
    name: 'monolith',
    config: {
      profile: 'monolith',
      modules: {
        'mango-authorization': {
          mode: 'local',
          runtimeCode: 'mango-admin-rbac-local',
        },
        ...commonModules,
        'mango-workflow': {
          mode: 'local',
          runtimeCode: 'mango-admin-workflow-local',
        },
        'mango-template': {
          mode: 'local',
          runtimeCode: 'mango-admin-template-local',
        },
      },
    },
  },
  {
    name: 'hybrid',
    config: {
      profile: 'hybrid',
      modules: {
        'mango-authorization': {
          mode: 'micro',
          runtimeCode: 'mango-admin-rbac-app',
          entry: 'http://b.mango.io:5181/',
        },
        ...commonModules,
        'mango-workflow': {
          mode: 'micro',
          runtimeCode: 'mango-admin-workflow-app',
          entry: 'http://c.mango.io:5182/',
        },
        'mango-template': {
          mode: 'micro',
          runtimeCode: 'mango-admin-template-app',
          entry: 'http://d.mango.io:5183/',
        },
      },
    },
  },
  {
    name: 'mixed',
    config: {
      profile: 'hybrid',
      modules: {
        'mango-authorization': {
          mode: 'micro',
          runtimeCode: 'mango-admin-rbac-app',
          entry: 'http://b.mango.io:5181/',
        },
        ...commonModules,
        'mango-workflow': {
          mode: 'local',
          runtimeCode: 'mango-admin-workflow-local',
        },
        'mango-template': {
          mode: 'micro',
          runtimeCode: 'mango-admin-template-app',
          entry: 'http://d.mango.io:5183/',
        },
      },
    },
  },
];

const pages = [
  {
    name: 'rbac-menu-package',
    path: '/#/system/menu-package',
    expected: ['新增套餐', '套餐名称'],
  },
  {
    name: 'workflow-start',
    path: '/#/workflow/start-process',
    expected: ['发起流程', '已发布流程'],
  },
  {
    name: 'template-categories',
    path: '/#/template/categories',
    expected: ['模板分类', '新增分类'],
  },
];

mkdirSync(screenshotDir, { recursive: true });
mkdirSync(reportDir, { recursive: true });

const originalRuntimeConfig = existsSync(runtimeConfigPath) ? readFileSync(runtimeConfigPath, 'utf-8') : '';
const report = {
  generatedAt: new Date().toISOString(),
  baseUrl,
  apiBaseUrl,
  modes: [],
  pass: false,
};

let browser;
try {
  browser = await chromium.launch({ channel: 'chrome', headless: true });
  const context = await browser.newContext({
    baseURL: baseUrl,
    viewport: { width: 1440, height: 900 },
    ignoreHTTPSErrors: true,
  });

  for (const mode of modes) {
    writeFileSync(runtimeConfigPath, `${JSON.stringify(mode.config, null, 2)}\n`);
    const page = await context.newPage();
    const consoleErrors = [];
    const pageErrors = [];
    const failedResponses = [];
    page.on('console', (message) => {
      if (message.type() === 'error' && !isIgnoredConsole(message.text())) {
        consoleErrors.push(message.text());
      }
    });
    page.on('pageerror', (error) => pageErrors.push(error.message));
    page.on('response', (response) => {
      if (response.status() >= 400 && !isIgnoredResponse(response.url())) {
        failedResponses.push({
          status: response.status(),
          url: response.url(),
        });
      }
    });

    const modeReport = {
      name: mode.name,
      config: mode.config,
      pages: [],
      consoleErrors,
      pageErrors,
      failedResponses,
      pass: false,
    };

    await login(page);

    for (const target of pages) {
      const url = new URL(target.path, baseUrl).toString();
      await page.goto(url, { waitUntil: 'domcontentloaded' });
      await page.waitForURL(`**${target.path}`, { timeout: 10000 });
      await page.locator('.shell-runtime-content').waitFor({ state: 'visible', timeout: 10000 });
      await page.waitForTimeout(1500);

      const screenshot = resolve(screenshotDir, `${mode.name}-${target.name}.png`);
      await page.screenshot({ path: screenshot, fullPage: true });

      const runtime = await readRuntime(page);
      const visibleTexts = await readVisibleTexts(page);
      const combinedText = visibleTexts.join(' ');
      const hasExpectedText = target.expected.every((text) => combinedText.includes(text));
      const hasFailureText = /404|页面加载失败|运行配置加载失败|缺少微应用运行配置|ENOTDIR/.test(combinedText);
      const shell = await readShellMetrics(page);
      const expectedRuntime = expectedRuntimeFor(mode.config, runtime.moduleCode);

      modeReport.pages.push({
        name: target.name,
        url,
        screenshot: relative(repoRoot, screenshot),
        expected: target.expected,
        hasExpectedText,
        hasFailureText,
        runtime,
        runtimeMatches: runtime.runtimeCode === expectedRuntime?.runtimeCode
          && runtime.pageType === (expectedRuntime?.mode === 'micro' ? 'MICRO_ROUTE' : 'LOCAL_ROUTE'),
        shell,
        visibleTextSample: combinedText.replace(/\s+/g, ' ').trim().slice(0, 500),
      });
    }

    modeReport.pass = modeReport.pages.every((item) =>
      item.hasExpectedText && !item.hasFailureText && item.runtimeMatches && item.shell.asideVisible && item.shell.topbarVisible
    ) && consoleErrors.length === 0 && pageErrors.length === 0 && failedResponses.length === 0;
    report.modes.push(modeReport);
    await page.close();
  }
} finally {
  if (browser) {
    await browser.close();
  }
  if (originalRuntimeConfig) {
    writeFileSync(runtimeConfigPath, originalRuntimeConfig);
  }
  report.pass = report.modes.every((mode) => mode.pass);
  writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
}

console.log(JSON.stringify(report, null, 2));
if (!report.pass) {
  process.exitCode = 1;
}

async function login(page) {
  await page.goto('/#/login', { waitUntil: 'domcontentloaded' });
  await page.getByPlaceholder('用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.getByPlaceholder('密码').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.getByRole('button', { name: /登\s*录/ }).click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
  await page.locator('.shell-runtime-content').waitFor({ state: 'visible', timeout: 10000 });
}

async function readRuntime(page) {
  return page.locator('.shell-runtime-content').evaluate((el) => ({
    moduleCode: el.dataset.mangoRuntimeModule || '',
    runtimeCode: el.dataset.mangoRuntimeCode || '',
    pageType: el.dataset.mangoRuntimePageType || '',
    entry: el.dataset.mangoRuntimeEntry || '',
  }));
}

async function readVisibleTexts(page) {
  return page.evaluate(() => {
    const texts = [
      document.body?.innerText || document.body?.textContent || '',
    ];
    document.querySelectorAll('iframe').forEach((frame) => {
      try {
        const body = frame.contentDocument?.body;
        if (body) {
          texts.push(body.innerText || body.textContent || '');
        }
      } catch {
        // Cross-origin iframes are not expected for wujie apps in local verification.
      }
    });
    document.querySelectorAll('wujie-app').forEach((app) => {
      const shadowText = app.shadowRoot?.textContent || '';
      if (shadowText) {
        texts.push(shadowText);
      }
    });
    return texts.filter(Boolean);
  });
}

async function readShellMetrics(page) {
  return page.evaluate(() => {
    const aside = document.querySelector('.layout-aside, .el-aside, aside');
    const topbar = document.querySelector('.layout-header, .top-bar, header');
    const activeMenus = Array.from(document.querySelectorAll('.is-active, .el-menu-item.is-active'))
      .map((el) => el.textContent?.trim())
      .filter(Boolean)
      .slice(0, 5);
    const frameDocuments = Array.from(document.querySelectorAll('iframe'))
      .map((frame) => {
        try {
          const doc = frame.contentDocument;
          return {
            buttonCount: doc ? doc.querySelectorAll('button, .el-button').length : 0,
            tableCount: doc ? doc.querySelectorAll('table, .el-table').length : 0,
          };
        } catch {
          return { buttonCount: 0, tableCount: 0 };
        }
      });
    return {
      asideVisible: Boolean(aside && getComputedStyle(aside).display !== 'none'),
      topbarVisible: Boolean(topbar && getComputedStyle(topbar).display !== 'none'),
      activeMenus,
      buttonCount: document.querySelectorAll('button, .el-button').length + frameDocuments.reduce((sum, item) => sum + item.buttonCount, 0),
      tableCount: document.querySelectorAll('table, .el-table').length + frameDocuments.reduce((sum, item) => sum + item.tableCount, 0),
      iframeCount: document.querySelectorAll('iframe').length,
    };
  });
}

function expectedRuntimeFor(config, moduleCode) {
  return config.modules[moduleCode];
}

function isIgnoredConsole(text) {
  return text.includes('favicon.ico') || text.includes('/api/admin/user/info') || text.includes('/api/auth/logout');
}

function isIgnoredResponse(url) {
  return url.includes('favicon.ico') || url.includes('/api/admin/user/info') || url.includes('/api/auth/logout');
}
