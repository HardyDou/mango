#!/usr/bin/env node
import { existsSync, mkdirSync, writeFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { dirname, join, resolve } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const scriptFile = fileURLToPath(import.meta.url);
const require = createRequire(import.meta.url);
const repoRoot = resolve(dirname(scriptFile), '..');
const workspaceRoot = resolve(repoRoot, '..');
const playwrightModulePath = resolvePlaywrightModulePath();
const args = parseArgs(process.argv.slice(2));
const evidenceRoot = resolve(args.evidenceDir || join(workspaceRoot, 'mango-docs/evidence/2026-05-29-admin-baseline'));
const baseUrl = args.baseUrl || `http://127.0.0.1:${args.frontendPort}`;
let devServerProcess;

try {
  mkdirSync(evidenceRoot, { recursive: true });
  ensurePlaywrightAvailable();
  log(`Evidence root: ${evidenceRoot}`);
  log(`Base URL: ${baseUrl}`);

  if (!args.useExternalWebServer) {
    devServerProcess = startMangoAdminDevServer();
    waitForHttp(baseUrl);
  } else {
    waitForHttp(baseUrl);
  }

  runBaselineBrowserCheck();
  writeSummary();
  log('Admin baseline E2E passed.');
} finally {
  stopProcess(devServerProcess);
}

function parseArgs(argv) {
  const parsed = {
    frontendPort: 7777,
    backendUrl: `http://127.0.0.1:${process.env.MANGO_BACKEND_PORT || 5555}`,
    baseUrl: '',
    evidenceDir: '',
    useExternalWebServer: false,
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--') {
      continue;
    }
    if (arg === '--frontend-port') {
      parsed.frontendPort = Number(argv[index + 1]);
      index += 1;
      continue;
    }
    if (arg === '--backend-url') {
      parsed.backendUrl = argv[index + 1] || '';
      index += 1;
      continue;
    }
    if (arg === '--base-url') {
      parsed.baseUrl = argv[index + 1] || '';
      index += 1;
      continue;
    }
    if (arg === '--evidence-dir') {
      parsed.evidenceDir = argv[index + 1] || '';
      index += 1;
      continue;
    }
    if (arg === '--use-external-web-server') {
      parsed.useExternalWebServer = true;
      continue;
    }
    fail(`Unknown argument: ${arg}`);
  }
  if (!Number.isInteger(parsed.frontendPort) || parsed.frontendPort <= 0) {
    fail(`Invalid frontend port: ${parsed.frontendPort}`);
  }
  if (!parsed.backendUrl) {
    fail('Missing --backend-url');
  }
  return parsed;
}

function startMangoAdminDevServer() {
  const child = spawn('pnpm', [
    '-F',
    'mango-admin',
    'dev',
    '--host',
    '127.0.0.1',
    '--port',
    String(args.frontendPort),
  ], {
    cwd: repoRoot,
    stdio: ['ignore', 'pipe', 'pipe'],
    env: {
      ...process.env,
      VITE_ADMIN_PROXY_PATH: args.backendUrl,
      VITE_PORT: String(args.frontendPort),
      VITE_HOST: '127.0.0.1',
      VITE_OPEN: 'false',
    },
  });
  pipeToFile(child.stdout, join(evidenceRoot, 'frontend-dev.out'));
  pipeToFile(child.stderr, join(evidenceRoot, 'frontend-dev.err'));
  return child;
}

function runBaselineBrowserCheck() {
  const browserScript = join(evidenceRoot, 'admin-baseline-browser-check.mjs');
  writeFileSync(browserScript, createBaselineBrowserScript(), 'utf8');
  run(process.execPath, [browserScript], {
    env: {
      ...process.env,
      MANGO_ADMIN_BASELINE_URL: baseUrl,
      MANGO_ADMIN_BASELINE_EVIDENCE: evidenceRoot,
      MANGO_PLAYWRIGHT_MODULE: playwrightModulePath,
    },
  });
}

function createBaselineBrowserScript() {
  return `
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.MANGO_PLAYWRIGHT_MODULE);
const baseUrl = process.env.MANGO_ADMIN_BASELINE_URL;
const evidenceRoot = process.env.MANGO_ADMIN_BASELINE_EVIDENCE;
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });
const consoleErrors = [];
const failedResponses = [];
const menuResponses = [];

page.on('console', (message) => {
  if (message.type() === 'error') {
    consoleErrors.push(message.text());
  }
});
page.on('pageerror', (error) => {
  consoleErrors.push(error.message);
});
page.on('response', async (response) => {
  if (response.status() >= 400) {
    failedResponses.push({ status: response.status(), url: response.url() });
  }
  const url = response.url();
  if (response.status() === 200 && url.includes('/api/authorization/menus/user') && url.includes('fmt=tree')) {
    try {
      menuResponses.push(await response.json());
    } catch {
      menuResponses.push({ parseError: true, url });
    }
  }
});

async function saveFailureEvidence(label) {
  await page.screenshot({ path: join(evidenceRoot, \`\${label}.png\`), fullPage: true }).catch(() => undefined);
  await page.content()
    .then((content) => writeFileSync(join(evidenceRoot, \`\${label}.html\`), content, 'utf8'))
    .catch(() => undefined);
}

async function login() {
  await page.goto(\`\${baseUrl}/#/login\`, { waitUntil: 'domcontentloaded' });
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined);
  if (await page.locator('input[placeholder="用户名"]').count() === 0) {
    await saveFailureEvidence('login-page-missing-username');
  }
  await page.locator('input[placeholder="用户名"]').fill('admin');
  await page.locator('input[placeholder="密码"]').fill('admin123');
  const tenantResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200,
    { timeout: 15000 },
  ).catch(() => null);
  await page.locator('input[placeholder="密码"]').blur();
  const tenantResponse = await tenantResponsePromise;
  if (tenantResponse) {
    await page.locator('.tenant-select').click();
    await page.getByRole('option', { name: /芒果集团/ }).click();
  }
  const menuResponsePromise = page.waitForResponse((response) => {
    const url = response.url();
    return response.status() === 200 && url.includes('/api/authorization/menus/user') && url.includes('fmt=tree');
  }, { timeout: 20000 });
  await page.locator('.login-btn, button:has-text("登 录")').first().click();
  await page.waitForURL('**/#/home', { timeout: 20000 });
  await menuResponsePromise;
}

async function waitForHomeReady() {
  await page.locator('.layout-container').waitFor({ state: 'visible', timeout: 15000 });
  await page.locator('.layout-navbars-container').waitFor({ state: 'visible', timeout: 15000 });
  await page.locator('.layout-aside, .layout-columns-aside').waitFor({ state: 'visible', timeout: 15000 });
  await page.locator('.layout-main').waitFor({ state: 'visible', timeout: 15000 });
  await page.waitForFunction(() => document.body.innerText.includes('用户总数'), null, { timeout: 15000 });
  await waitForNoTransientMessages();
}

async function waitForNoTransientMessages() {
  await page.waitForFunction(() => !document.querySelector('.el-message'), null, { timeout: 8000 }).catch(() => undefined);
}

async function waitForUserDropdown() {
  await waitForVisibleText('.el-dropdown__popper', '个人中心');
  await page.waitForTimeout(300);
}

async function waitForSettingsDrawer() {
  await waitForVisibleText('.el-drawer', '布局配置');
  await page.waitForTimeout(300);
}

async function waitForOverlayClosed() {
  await waitForHiddenText('.el-dropdown__popper', '个人中心');
  await waitForHiddenText('.el-drawer', '布局配置');
  await waitForNoTransientMessages();
}

async function closeUserDropdown() {
  const attempts = [
    () => page.locator('.layout-breadcrumb-user').click(),
    () => page.keyboard.press('Escape'),
    () => page.mouse.click(400, 400),
  ];
  for (const attempt of attempts) {
    await attempt();
    const closed = await isHiddenText('.el-dropdown__popper', '个人中心');
    if (closed) {
      return;
    }
  }
  await waitForHiddenText('.el-dropdown__popper', '个人中心');
}

async function waitForVisibleText(selector, text) {
  await page.waitForFunction(({ selector: targetSelector, text: targetText }) => {
    const isVisible = (element) => {
      const rect = element.getBoundingClientRect();
      const style = window.getComputedStyle(element);
      return rect.width > 0
        && rect.height > 0
        && rect.right > 0
        && rect.bottom > 0
        && rect.left < window.innerWidth
        && rect.top < window.innerHeight
        && Number(style.opacity || '1') >= 0.95
        && style.display !== 'none'
        && style.visibility !== 'hidden';
    };
    return Array.from(document.querySelectorAll(targetSelector)).some((element) => {
      return isVisible(element) && (element.textContent || '').includes(targetText);
    });
  }, { selector, text }, { timeout: 8000 });
}

async function waitForHiddenText(selector, text) {
  await page.waitForFunction(({ selector: targetSelector, text: targetText }) => {
    const isVisible = (element) => {
      const rect = element.getBoundingClientRect();
      const style = window.getComputedStyle(element);
      return rect.width > 0
        && rect.height > 0
        && rect.right > 0
        && rect.bottom > 0
        && rect.left < window.innerWidth
        && rect.top < window.innerHeight
        && Number(style.opacity || '1') >= 0.95
        && style.display !== 'none'
        && style.visibility !== 'hidden';
    };
    return !Array.from(document.querySelectorAll(targetSelector)).some((element) => {
      return isVisible(element) && (element.textContent || '').includes(targetText);
    });
  }, { selector, text }, { timeout: 8000 });
}

async function isHiddenText(selector, text) {
  return page.evaluate(({ selector: targetSelector, text: targetText }) => {
    const isVisible = (element) => {
      const rect = element.getBoundingClientRect();
      const style = window.getComputedStyle(element);
      return rect.width > 0
        && rect.height > 0
        && rect.right > 0
        && rect.bottom > 0
        && rect.left < window.innerWidth
        && rect.top < window.innerHeight
        && Number(style.opacity || '1') >= 0.95
        && style.display !== 'none'
        && style.visibility !== 'hidden';
    };
    return !Array.from(document.querySelectorAll(targetSelector)).some((element) => {
      return isVisible(element) && (element.textContent || '').includes(targetText);
    });
  }, { selector, text });
}

async function assertScreenshotState(label, expectedText, options = {}) {
  const state = await page.evaluate(({ expectedText: expected, forbiddenText: forbidden }) => {
    const isVisible = (element) => {
      const rect = element.getBoundingClientRect();
      const style = window.getComputedStyle(element);
      return rect.width > 0
        && rect.height > 0
        && rect.right > 0
        && rect.bottom > 0
        && rect.left < window.innerWidth
        && rect.top < window.innerHeight
        && Number(style.opacity || '1') >= 0.95
        && style.display !== 'none'
        && style.visibility !== 'hidden';
    };
    const bodyText = document.body.innerText;
    return {
      hasExpectedText: bodyText.includes(expected),
      hasForbiddenText: forbidden ? bodyText.includes(forbidden) : false,
      visibleDrawer: Array.from(document.querySelectorAll('.el-drawer')).some((element) => {
        return isVisible(element);
      }),
      visibleDropdown: Array.from(document.querySelectorAll('.el-dropdown__popper')).some((element) => {
        return isVisible(element);
      }),
    };
  }, { expectedText, forbiddenText: options.forbiddenText || '' });
  if (!state.hasExpectedText
    || state.hasForbiddenText
    || (options.requireDropdown && !state.visibleDropdown)
    || (options.requireDrawer && !state.visibleDrawer)
    || (options.forbidDropdown && state.visibleDropdown)
    || (options.forbidDrawer && state.visibleDrawer)) {
    throw new Error(\`Unexpected screenshot state for \${label}: \${JSON.stringify(state)}\`);
  }
}

function flattenMenus(menus) {
  return (menus || []).flatMap((menu) => [menu, ...flattenMenus(menu.children || [])]);
}

async function elementReport(selector) {
  return page.evaluate((targetSelector) => {
    const element = document.querySelector(targetSelector);
    if (!element) {
      return { selector: targetSelector, exists: false, visible: false, rect: null, style: null, text: '' };
    }
    const rect = element.getBoundingClientRect();
    const style = window.getComputedStyle(element);
    return {
      selector: targetSelector,
      exists: true,
      visible: style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0,
      text: (element.textContent || '').trim().replace(/\\s+/g, ' ').slice(0, 200),
      rect: {
        x: Math.round(rect.x),
        y: Math.round(rect.y),
        width: Math.round(rect.width),
        height: Math.round(rect.height),
        top: Math.round(rect.top),
        bottom: Math.round(rect.bottom),
      },
      style: {
        display: style.display,
        backgroundColor: style.backgroundColor,
        color: style.color,
        border: style.border,
        fontSize: style.fontSize,
      },
    };
  }, selector);
}

async function collectReport() {
  const bodyText = await page.locator('body').innerText();
  const menuTree = menuResponses.at(-1)?.data || [];
  const menus = flattenMenus(menuTree).filter((menu) => menu.menuType !== 3 && menu.visible !== 0);
  const menuNames = menus.map((menu) => menu.menuName);
  const css = await page.evaluate(() => {
    const styleSheets = Array.from(document.styleSheets).map((sheet) => {
      try {
        return { href: sheet.href, rules: sheet.cssRules?.length ?? null };
      } catch {
        return { href: sheet.href, inaccessible: true };
      }
    });
    return {
      styleSheets,
      brokenLinks: styleSheets.filter((item) => item.href && item.rules === 0),
      primaryColor: getComputedStyle(document.documentElement).getPropertyValue('--mango-color-primary').trim(),
      navBackground: getComputedStyle(document.querySelector('.layout-navbars-container') || document.body).backgroundColor,
      menuBackground: getComputedStyle(document.querySelector('.layout-aside') || document.body).backgroundColor,
    };
  });
  const elements = {
    layout: await elementReport('.layout-container'),
    navbars: await elementReport('.layout-navbars-container'),
    aside: await elementReport('.layout-aside, .layout-columns-aside'),
    main: await elementReport('.layout-main'),
    tags: await elementReport('.tags-view-container'),
    breadcrumb: await elementReport('.layout-main-breadcrumb, .layout-breadcrumb'),
    settings: await elementReport('.layout-breadcrumb-settings'),
    user: await elementReport('.layout-breadcrumb-user'),
    topSystem: await elementReport('.layout-top-system-item'),
  };
  const horizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 2);
  const requiredMenuNames = [
    '系统管理',
    '权限管理',
    '日志管理',
    '审批中心',
    '流程办理',
    '平台能力',
    '日历管理',
    '编号规则',
    '文件管理',
    '模板管理',
  ];
  const checks = {
    hasLayout: elements.layout.visible,
    hasNavbars: elements.navbars.visible && elements.navbars.rect.height >= 48,
    hasAside: elements.aside.visible && elements.aside.rect.width >= 180,
    hasMain: elements.main.visible,
    hasTags: elements.tags.visible,
    hasUserArea: elements.user.visible,
    hasSettingsEntry: elements.settings.visible,
    hasPrimaryColor: css.primaryColor === '#2E5CF6',
    hasNoBrokenCssLinks: css.brokenLinks.length === 0,
    hasNoHorizontalOverflow: !horizontalOverflow,
    hasBackendMenuTree: menuTree.length > 0,
    hasRequiredMenus: requiredMenuNames.every((name) => menuNames.includes(name)),
    hasHomeText: bodyText.includes('首页'),
  };
  return {
    url: page.url(),
    viewport: await page.viewportSize(),
    checks,
    requiredMenuNames,
    menuNames,
    menuTree,
    elements,
    css,
    browser: {
      consoleErrors,
      failedResponses,
    },
  };
}

await login();
await waitForHomeReady();
await assertScreenshotState('home', '用户总数', { forbiddenText: '登录成功', forbidDropdown: true, forbidDrawer: true });
await page.screenshot({ path: join(evidenceRoot, 'home-1440x960.png'), fullPage: true });
await page.locator('.layout-breadcrumb-user').click();
await waitForUserDropdown();
await assertScreenshotState('user-dropdown', '个人中心', { requireDropdown: true, forbidDrawer: true });
await page.screenshot({ path: join(evidenceRoot, 'user-dropdown-1440x960.png'), fullPage: true });
await closeUserDropdown();
await waitForOverlayClosed();
await page.locator('.layout-breadcrumb-settings').click();
await waitForSettingsDrawer();
await assertScreenshotState('settings-drawer', '布局配置', { forbiddenText: '个人中心', requireDrawer: true, forbidDropdown: true });
await page.screenshot({ path: join(evidenceRoot, 'settings-drawer-1440x960.png'), fullPage: true });
await page.keyboard.press('Escape');
await page.mouse.click(400, 400);
await waitForOverlayClosed();

const report = await collectReport();
const failedChecks = Object.entries(report.checks).filter(([, passed]) => !passed).map(([name]) => name);
if (failedChecks.length > 0) {
  throw new Error(\`Admin baseline checks failed: \${failedChecks.join(', ')}. Report: \${JSON.stringify(report)}\`);
}
const unexpectedResponses = failedResponses.filter((item) => !item.url.endsWith('/favicon.ico'));
if (unexpectedResponses.length > 0) {
  throw new Error(\`Unexpected failed browser responses:\\n\${unexpectedResponses.map(item => \`\${item.status} \${item.url}\`).join('\\n')}\`);
}
const unexpectedErrors = consoleErrors.filter((message) => !message.includes('ResizeObserver loop'));
if (unexpectedErrors.length > 0) {
  throw new Error(\`Unexpected browser console errors:\\n\${unexpectedErrors.join('\\n')}\`);
}

writeFileSync(join(evidenceRoot, 'layout-report.json'), \`\${JSON.stringify(report, null, 2)}\\n\`, 'utf8');
await browser.close();
`;
}

function writeSummary() {
  const summary = [
    '# Mango Admin Baseline E2E',
    '',
    `- Base URL: ${baseUrl}`,
    `- Backend URL: ${args.backendUrl}`,
    '- Checks: original Mango Admin login, backend menu tree, shell layout, top bar, user area, settings entry, tags, theme color, CSS links, screenshots',
    '- Evidence: home-1440x960.png, user-dropdown-1440x960.png, settings-drawer-1440x960.png, layout-report.json',
    '',
  ].join('\n');
  writeFileSync(join(evidenceRoot, 'summary.md'), summary, 'utf8');
}

function run(command, commandArgs, options = {}) {
  log(`$ ${command} ${commandArgs.join(' ')}`);
  const result = spawnSync(command, commandArgs, {
    cwd: options.cwd || repoRoot,
    env: options.env || process.env,
    stdio: 'inherit',
    encoding: 'utf8',
  });
  if (result.status !== 0) {
    fail(`Command failed: ${command} ${commandArgs.join(' ')}`);
  }
  return result;
}

function waitForHttp(url) {
  const deadline = Date.now() + 120_000;
  while (Date.now() < deadline) {
    const result = spawnSync('curl', ['-fsS', url], { stdio: 'ignore' });
    if (result.status === 0) {
      return;
    }
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 500);
  }
  fail(`Timed out waiting for ${url}`);
}

function pipeToFile(stream, filePath) {
  stream.on('data', (chunk) => {
    writeFileSync(filePath, chunk, { flag: 'a' });
  });
}

function stopProcess(child) {
  if (!child || child.killed) {
    return;
  }
  child.kill('SIGTERM');
}

function resolvePlaywrightModulePath() {
  const directPath = join(repoRoot, 'node_modules/.pnpm/playwright@1.59.1/node_modules/playwright');
  if (existsSync(directPath)) {
    return directPath;
  }
  return dirname(require.resolve('playwright/package.json'));
}

function ensurePlaywrightAvailable() {
  if (!existsSync(playwrightModulePath)) {
    fail(`Playwright module not found: ${playwrightModulePath}`);
  }
}

function log(message) {
  console.log(`[admin-baseline-e2e] ${message}`);
}

function fail(message) {
  throw new Error(message);
}
