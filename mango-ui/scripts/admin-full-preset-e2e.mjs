#!/usr/bin/env node
import {
  cpSync,
  existsSync,
  mkdirSync,
  mkdtempSync,
  readFileSync,
  readdirSync,
  rmSync,
  writeFileSync,
} from 'node:fs';
import { createRequire } from 'node:module';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const scriptFile = fileURLToPath(import.meta.url);
const require = createRequire(import.meta.url);
const repoRoot = resolve(dirname(scriptFile), '..');
const workspaceRoot = resolve(repoRoot, '..');
const packagesRoot = join(repoRoot, 'packages');
const playwrightModulePath = resolvePlaywrightModulePath();
const args = parseArgs(process.argv.slice(2));
const tempRoot = args.keepTemp
  ? mkdirAndReturn(resolve(tmpdir(), `mango-admin-full-preset-e2e-${Date.now()}`))
  : mkdtempSync(join(tmpdir(), 'mango-admin-full-preset-e2e-'));
const appRoot = join(tempRoot, 'admin-full-preset-app');
const stagedPackageRoot = join(tempRoot, 'staged-packages');
const evidenceRoot = resolve(args.evidenceDir || join(workspaceRoot, 'mango-docs/evidence/2026-05-29-admin-full-preset'));
const baseUrl = args.baseUrl || `http://127.0.0.1:${args.frontendPort}`;
let devServerProcess;

try {
  mkdirSync(evidenceRoot, { recursive: true });
  ensurePlaywrightAvailable();
  log(`Temporary root: ${tempRoot}`);
  log(`Evidence root: ${evidenceRoot}`);
  log(`Base URL: ${baseUrl}`);
  log(`Backend URL: ${args.backendUrl}`);

  if (!args.skipPackageBuild) {
    run('pnpm', ['package:build'], { cwd: repoRoot });
  }

  const stagedPackages = stageLocalMangoPackages();
  createFullPresetApp(stagedPackages);
  run('pnpm', ['install', '--ignore-scripts'], {
    cwd: appRoot,
    captureFile: join(evidenceRoot, 'install.out'),
  });
  run('pnpm', ['exec', 'vite', 'build'], {
    cwd: appRoot,
    captureFile: join(evidenceRoot, 'vite-build.out'),
  });

  if (!args.useExternalWebServer) {
    devServerProcess = startFullPresetDevServer();
    waitForHttp(baseUrl);
  } else {
    waitForHttp(baseUrl);
  }

  runBrowserCheck();
  writeSummary();
  log('Admin full preset E2E passed.');
} finally {
  stopProcess(devServerProcess);
  if (!args.keepTemp) {
    rmSync(tempRoot, { recursive: true, force: true });
  } else {
    log(`Kept temporary root: ${tempRoot}`);
  }
}

function parseArgs(argv) {
  const parsed = {
    frontendPort: 7788,
    backendUrl: `http://127.0.0.1:${process.env.MANGO_BACKEND_PORT || 5555}`,
    baseUrl: '',
    evidenceDir: '',
    useExternalWebServer: false,
    skipPackageBuild: false,
    keepTemp: false,
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--') continue;
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
    if (arg === '--skip-package-build') {
      parsed.skipPackageBuild = true;
      continue;
    }
    if (arg === '--keep-temp') {
      parsed.keepTemp = true;
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

function mkdirAndReturn(path) {
  mkdirSync(path, { recursive: true });
  return path;
}

function stageLocalMangoPackages() {
  mkdirSync(stagedPackageRoot, { recursive: true });
  const staged = new Map();
  const packages = [];

  for (const entry of readdirSync(packagesRoot)) {
    const packageDir = join(packagesRoot, entry);
    const packageJsonPath = join(packageDir, 'package.json');
    const distDir = join(packageDir, 'dist');
    if (!existsSync(packageJsonPath) || !existsSync(distDir)) {
      continue;
    }
    const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
    if (!packageJson.name?.startsWith('@mango/')) {
      continue;
    }
    const targetDir = join(stagedPackageRoot, packageJson.name.replace('@', '').replace('/', '-'));
    packages.push({ packageJson, distDir, targetDir });
    staged.set(packageJson.name, targetDir);
  }

  if (!staged.has('@mango/admin')) {
    fail('Built @mango/admin package not found. Run pnpm package:build first.');
  }

  for (const { packageJson, distDir, targetDir } of packages) {
    rmSync(targetDir, { recursive: true, force: true });
    mkdirSync(targetDir, { recursive: true });
    cpSync(distDir, join(targetDir, 'dist'), { recursive: true });
    const stagedPackageJson = { ...packageJson };
    delete stagedPackageJson.scripts;
    delete stagedPackageJson.devDependencies;
    for (const section of ['dependencies', 'peerDependencies', 'optionalDependencies']) {
      if (!stagedPackageJson[section]) continue;
      for (const dependencyName of Object.keys(stagedPackageJson[section])) {
        if (staged.has(dependencyName)) {
          stagedPackageJson[section][dependencyName] = `file:${staged.get(dependencyName)}`;
        }
      }
    }
    writeFileSync(join(targetDir, 'package.json'), `${JSON.stringify(stagedPackageJson, null, 2)}\n`, 'utf8');
  }

  return staged;
}

function createFullPresetApp(stagedPackages) {
  mkdirSync(join(appRoot, 'src'), { recursive: true });
  mkdirSync(join(appRoot, 'public'), { recursive: true });
  writeFileSync(join(appRoot, 'index.html'), '<div id="app"></div><script type="module" src="/src/main.ts"></script>\n', 'utf8');
  writeFileSync(join(appRoot, 'public/mango-runtime-config.json'), `${JSON.stringify({
    profile: 'monolith',
    modules: {
      'mango-authorization': {
        mode: 'local',
        runtimeCode: 'mango-admin-rbac-app',
        appType: 'LOCAL',
        framework: 'vue3',
      },
      'mango-system': {
        mode: 'local',
        runtimeCode: 'mango-admin-system-local',
        appType: 'LOCAL',
        framework: 'vue3',
      },
      'mango-workflow': {
        mode: 'local',
        runtimeCode: 'mango-admin-workflow-local',
        appType: 'LOCAL',
        framework: 'vue3',
      },
    },
  }, null, 2)}\n`, 'utf8');
  writeFileSync(join(appRoot, 'src/main.ts'), [
    "import { createMangoAdmin } from '@mango/admin';",
    "import '@mango/admin/style.css';",
    '',
    '(globalThis as any).__MANGO_IMPORT_META_ENV__ = import.meta.env;',
    "(window as any).__MANGO_ADMIN_FULL_PRESET_E2E__ = 'createMangoAdmin:@mango/admin';",
    '',
    'createMangoAdmin({',
    "  preset: 'full',",
    "  mountTarget: '#app',",
    "  apiBaseUrl: '/api',",
    "  title: 'Mango Admin Full Preset',",
    "  runtimeConfigUrl: '/mango-runtime-config.json',",
    '  menu: {',
    "    onMergeReport: (report) => { (window as any).__MANGO_ADMIN_FULL_MENU_MERGE_REPORT__ = report; },",
    '  },',
    '}).mount();',
    '',
  ].join('\n'), 'utf8');
  writeFileSync(join(appRoot, 'vite.config.mjs'), [
    "import { defineConfig } from 'vite';",
    "import vue from '@vitejs/plugin-vue';",
    '',
    'export default defineConfig({',
    '  plugins: [vue()],',
    "  resolve: { dedupe: ['vue', 'vue-router', 'pinia', 'element-plus', '@element-plus/icons-vue', 'dayjs'] },",
    '  optimizeDeps: {',
    "    include: ['dayjs', 'dayjs/plugin/advancedFormat.js', 'dayjs/plugin/customParseFormat.js', 'dayjs/plugin/dayOfYear.js', 'dayjs/plugin/isSameOrAfter.js', 'dayjs/plugin/isSameOrBefore.js', 'dayjs/plugin/localeData.js', 'dayjs/plugin/weekOfYear.js', 'dayjs/plugin/weekYear.js', 'element-plus/es/locale/lang/zh-cn', 'element-plus/es/locale/lang/en'],",
    '  },',
    '  server: {',
    '    proxy: {',
    "      '/api': {",
    '        target: process.env.MANGO_FULL_PRESET_BACKEND_URL,',
    '        changeOrigin: true,',
    "        rewrite: (path) => path.replace(/^\\/api/, ''),",
    '      },',
    '    },',
    '  },',
    '});',
    '',
  ].join('\n'), 'utf8');
  writeFileSync(join(appRoot, 'package.json'), `${JSON.stringify({
    name: 'admin-full-preset-e2e-app',
    private: true,
    type: 'module',
    scripts: {
      build: 'vite build',
      dev: 'vite',
    },
    dependencies: {
      '@mango/admin': `file:${stagedPackages.get('@mango/admin')}`,
      '@vitejs/plugin-vue': '^5.2.1',
      vite: '^5.1.4',
      typescript: '^5.3.3',
      vue: '^3.5.13',
      'vue-router': '^4.1.6',
      pinia: '^2.3.0',
      'pinia-plugin-persistedstate': '^4.5.0',
      'element-plus': '2.14.0',
      '@element-plus/icons-vue': '2.3.2',
      dayjs: '^1.11.10',
      'vue-i18n': '^9.2.0',
    },
  }, null, 2)}\n`, 'utf8');
}

function startFullPresetDevServer() {
  const child = spawn('pnpm', [
    'exec',
    'vite',
    '--host',
    '127.0.0.1',
    '--port',
    String(args.frontendPort),
  ], {
    cwd: appRoot,
    stdio: ['ignore', 'pipe', 'pipe'],
    env: {
      ...process.env,
      MANGO_FULL_PRESET_BACKEND_URL: args.backendUrl,
      VITE_MANGO_E2E: 'true',
    },
  });
  pipeToFile(child.stdout, join(evidenceRoot, 'frontend-dev.out'));
  pipeToFile(child.stderr, join(evidenceRoot, 'frontend-dev.err'));
  return child;
}

function runBrowserCheck() {
  const browserScript = join(evidenceRoot, 'admin-full-preset-browser-check.mjs');
  writeFileSync(browserScript, createBrowserScript(), 'utf8');
  run(process.execPath, [browserScript], {
    env: {
      ...process.env,
      MANGO_ADMIN_FULL_PRESET_URL: baseUrl,
      MANGO_ADMIN_FULL_PRESET_EVIDENCE: evidenceRoot,
      MANGO_PLAYWRIGHT_MODULE: playwrightModulePath,
    },
  });
}

function createBrowserScript() {
  return `
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.MANGO_PLAYWRIGHT_MODULE);
const baseUrl = process.env.MANGO_ADMIN_FULL_PRESET_URL;
const evidenceRoot = process.env.MANGO_ADMIN_FULL_PRESET_EVIDENCE;
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });
const consoleErrors = [];
const failedResponses = [];
const menuResponses = [];
const apiResponses = [];

page.on('console', (message) => {
  if (message.type() === 'error') consoleErrors.push(message.text());
});
page.on('pageerror', (error) => consoleErrors.push(error.message));
page.on('response', async (response) => {
  const url = response.url();
  if (url.includes('/api/')) {
    apiResponses.push({
      status: response.status(),
      method: response.request().method(),
      url,
    });
  }
  if (response.status() >= 400) {
    failedResponses.push({ status: response.status(), url: response.url() });
  }
  if (response.status() === 200 && url.includes('/api/authorization/menus/user') && url.includes('fmt=tree')) {
    try {
      menuResponses.push(await response.json());
    } catch {
      menuResponses.push({ parseError: true, url });
    }
  }
});

function writeApiResponses(label) {
  writeFileSync(join(evidenceRoot, \`\${label}.json\`), \`\${JSON.stringify(apiResponses, null, 2)}\\n\`, 'utf8');
}

async function saveFailureEvidence(label) {
  await page.screenshot({ path: join(evidenceRoot, \`\${label}.png\`), fullPage: true }).catch(() => undefined);
  await page.content()
    .then((content) => writeFileSync(join(evidenceRoot, \`\${label}.html\`), content, 'utf8'))
    .catch(() => undefined);
  writeApiResponses(\`\${label}-api-responses\`);
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
  }, { timeout: 20000 }).catch(async (error) => {
    await saveFailureEvidence('menu-response-timeout');
    throw error;
  });
  await page.locator('.login-btn, button:has-text("登 录")').first().click();
  await page.waitForURL('**/#/home', { timeout: 20000 }).catch(async (error) => {
    await saveFailureEvidence('login-url-timeout');
    throw error;
  });
  await menuResponsePromise;
}

async function waitForHomeReady() {
  try {
    await page.locator('.layout-container').waitFor({ state: 'visible', timeout: 15000 });
    await page.locator('.layout-navbars-container').waitFor({ state: 'visible', timeout: 15000 });
    await page.locator('.layout-aside, .layout-columns-aside').waitFor({ state: 'visible', timeout: 15000 });
    await page.locator('.layout-main').waitFor({ state: 'visible', timeout: 15000 });
    await page.waitForFunction(() => document.body.innerText.includes('用户总数'), null, { timeout: 15000 });
    await waitForNoTransientMessages();
  } catch (error) {
    await saveFailureEvidence('home-ready-timeout');
    throw error;
  }
}

async function waitForNoTransientMessages() {
  await page.waitForFunction(() => !document.querySelector('.el-message'), null, { timeout: 8000 }).catch(() => undefined);
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

async function waitForOverlayClosed() {
  await page.locator('.el-dropdown__popper:has-text("个人中心")').first().waitFor({ state: 'hidden', timeout: 8000 }).catch(() => undefined);
  await page.locator('.el-drawer:has-text("布局配置")').first().waitFor({ state: 'hidden', timeout: 8000 }).catch(() => undefined);
  await waitForNoTransientMessages();
}

async function closeUserDropdown() {
  const dropdown = page.locator('.el-dropdown__popper:has-text("个人中心")').first();
  await page.keyboard.press('Escape');
  await dropdown.waitFor({ state: 'hidden', timeout: 2000 }).catch(async () => {
    await page.locator('.layout-breadcrumb-user').click();
    await dropdown.waitFor({ state: 'hidden', timeout: 3000 }).catch(async () => {
      await page.mouse.click(20, 160);
      await dropdown.waitFor({ state: 'hidden', timeout: 3000 });
    });
  });
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
    const visibleDrawers = Array.from(document.querySelectorAll('.el-drawer'))
      .filter(isVisible)
      .map((element) => {
        const rect = element.getBoundingClientRect();
        return {
          text: element.textContent || '',
          left: Math.round(rect.left),
          right: Math.round(rect.right),
          width: Math.round(rect.width),
          height: Math.round(rect.height),
          visibleWidth: Math.round(Math.min(rect.right, window.innerWidth) - Math.max(rect.left, 0)),
        };
      });
    const bodyText = document.body.innerText;
    return {
      hasExpectedText: bodyText.includes(expected),
      hasForbiddenText: forbidden ? bodyText.includes(forbidden) : false,
      visibleDrawer: visibleDrawers.length > 0,
      visibleDrawers,
      visibleDropdown: Array.from(document.querySelectorAll('.el-dropdown__popper')).some(isVisible),
    };
  }, { expectedText, forbiddenText: options.forbiddenText || '' });
  const hasFullyVisibleDrawer = state.visibleDrawers?.some((drawer) => {
    return drawer.text.includes(expectedText)
      && drawer.width >= 260
      && drawer.visibleWidth >= 260
      && drawer.left >= 0
      && drawer.right <= 1442;
  }) ?? false;
  if (!state.hasExpectedText
    || state.hasForbiddenText
    || (options.requireDropdown && !state.visibleDropdown)
    || (options.requireDrawer && !state.visibleDrawer)
    || (options.requireDrawerFullyVisible && !hasFullyVisibleDrawer)
    || (options.forbidDropdown && state.visibleDropdown)
    || (options.forbidDrawer && state.visibleDrawer)) {
    throw new Error(\`Unexpected screenshot state for \${label}: \${JSON.stringify(state)}\`);
  }
}

async function waitForDrawerFullyVisible(title) {
  await page.waitForFunction((expectedTitle) => {
    const isVisible = (element) => {
      const rect = element.getBoundingClientRect();
      const style = window.getComputedStyle(element);
      const visibleWidth = Math.min(rect.right, window.innerWidth) - Math.max(rect.left, 0);
      return (element.textContent || '').includes(expectedTitle)
        && rect.width >= 260
        && visibleWidth >= 260
        && rect.left >= 0
        && rect.right <= window.innerWidth + 2
        && Number(style.opacity || '1') >= 0.95
        && style.display !== 'none'
        && style.visibility !== 'hidden';
    };
    return Array.from(document.querySelectorAll('.el-drawer')).some(isVisible);
  }, title, { timeout: 8000 });
}

function flattenMenus(menus) {
  return (menus || []).flatMap((menu) => [menu, ...flattenMenus(menu.children || [])]);
}

function sanitizeFileName(value) {
  return String(value || 'menu')
    .replace(/[^a-zA-Z0-9\u4e00-\u9fa5_-]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 80) || 'menu';
}

function isPageMenu(menu) {
  return menu
    && menu.visible !== 0
    && menu.menuType === 2
    && typeof menu.path === 'string'
    && menu.path.startsWith('/')
    && menu.path !== '/home'
    && !menu.externalUrl;
}

function collectPageMenus(menu) {
  const direct = isPageMenu(menu) ? [menu] : [];
  return [
    ...direct,
    ...(menu.children || []).flatMap(collectPageMenus),
  ];
}

function collectMenuSamples(menuTree) {
  return (menuTree || [])
    .filter((menu) => menu.visible !== 0 && menu.menuType !== 3)
    .map((topMenu) => ({
      topMenu,
      children: collectPageMenus(topMenu).slice(0, 3),
    }))
    .filter((sample) => sample.children.length > 0);
}

async function navigateToMenu(menu) {
  await page.goto(\`\${baseUrl}/#\${menu.path}\`, { waitUntil: 'domcontentloaded' });
  await page.locator('.layout-container').waitFor({ state: 'visible', timeout: 15000 });
  await page.locator('.layout-main').waitFor({ state: 'visible', timeout: 15000 });
  await page.locator('.shell-runtime-content').waitFor({ state: 'visible', timeout: 15000 });
  await page.waitForLoadState('networkidle', { timeout: 12000 }).catch(() => undefined);
  await page.waitForFunction(() => {
    const content = document.querySelector('.shell-runtime-content');
    if (!content) return false;
    return content.children.length > 0 || (content.textContent || '').trim().length > 0;
  }, null, { timeout: 15000 }).catch(() => undefined);
  await waitForNoTransientMessages();
}

function expectedContentTextForMenu(menu) {
  const component = menu.component || '';
  const path = menu.path || '';
  const menuName = menu.menuName || menu.menuCode || '';
  const rules = [
    { pattern: 'menu-package', text: '套餐名称' },
    { pattern: 'tenant', text: '机构名称' },
    { pattern: 'org', text: 'MANGO_TECH' },
    { pattern: 'post', text: '岗位名称' },
    { pattern: 'user', text: '用户名' },
    { pattern: 'role', text: '角色名称' },
    { pattern: 'menu/index', text: '菜单名称' },
    { pattern: 'app/index', text: '应用名称' },
    { pattern: 'dict', text: '字典名称' },
    { pattern: 'param', text: '参数' },
    { pattern: 'config', text: '参数' },
    { pattern: 'area', text: '行政区划' },
    { pattern: 'start-process', text: '流程' },
    { pattern: 'task-list', text: '流程' },
    { pattern: 'task/todo', text: '流程' },
    { pattern: 'task/initiated', text: '流程' },
    { pattern: 'calendar', text: '日历' },
    { pattern: 'numgen', text: '编号' },
    { pattern: 'file/files', text: '文件' },
    { pattern: 'notice/site-message', text: '消息' },
    { pattern: 'notice/message-definition', text: '消息' },
    { pattern: 'notice/send-message', text: '发送' },
  ];
  return rules.find((rule) => component.includes(rule.pattern) || path.includes(rule.pattern))?.text || menuName;
}

async function waitForRuntimeContentReady(expectedText) {
  await page.waitForFunction((expected) => {
    const isVisible = (element) => {
      const rect = element.getBoundingClientRect();
      const style = window.getComputedStyle(element);
      return rect.width > 0
        && rect.height > 0
        && rect.right > 0
        && rect.bottom > 0
        && rect.left < window.innerWidth
        && rect.top < window.innerHeight
        && style.display !== 'none'
        && style.visibility !== 'hidden';
    };
    const content = document.querySelector('.shell-runtime-content');
    if (!content || !isVisible(content)) return false;
    const contentText = (content.innerText || '').trim();
    const hasRuntimeError = ['页面加载失败', '运行配置加载失败', '缺少微应用运行配置', 'The requested module', 'does not provide an export named', '404']
      .some((pattern) => contentText.includes(pattern));
    if (hasRuntimeError) return true;
    if (expected && contentText.includes(expected)) return true;
    return !expected && contentText.length > 0;
  }, expectedText, { timeout: 15000 }).catch(() => undefined);
  await waitForNoTransientMessages();
}

async function getRuntimeContentState(expectedText) {
  return page.evaluate((expected) => {
    const isVisible = (element) => {
      const rect = element.getBoundingClientRect();
      const style = window.getComputedStyle(element);
      return rect.width > 0
        && rect.height > 0
        && rect.right > 0
        && rect.bottom > 0
        && rect.left < window.innerWidth
        && rect.top < window.innerHeight
        && style.display !== 'none'
        && style.visibility !== 'hidden';
    };
    const content = document.querySelector('.shell-runtime-content');
    const main = document.querySelector('.layout-main');
    const contentText = (content?.innerText || '').trim();
    const forbiddenPatterns = ['页面加载失败', '运行配置加载失败', '缺少微应用运行配置', 'The requested module', 'does not provide an export named', '404'];
    const visibleRuntimeError = Array.from(content?.querySelectorAll('.micro-runtime-empty') || []).some(isVisible);
    const visibleContentBlocks = Array.from(content?.querySelectorAll([
      '.el-card',
      '.el-table',
      '.el-form',
      '.el-empty',
      '.el-descriptions',
      '.workflow-launch-board',
      '.calendar-layout',
      '.file-container',
      '.notice-send-message-page',
      '.tenant-container',
      '.menu-package-container',
      '.business-reports-page',
    ].join(',')) || []).filter(isVisible);
    return {
      url: location.href,
      hasLayout: Boolean(document.querySelector('.layout-container')),
      hasAside: Boolean(document.querySelector('.layout-aside, .layout-columns-aside')),
      hasMain: Boolean(main) && isVisible(main),
      hasRuntimeContent: Boolean(content) && isVisible(content),
      runtimeChildCount: content?.children.length || 0,
      runtimeTextLength: contentText.length,
      runtimeTextSample: contentText.slice(0, 500),
      hasExpectedContentText: expected ? contentText.includes(expected) : contentText.length > 0,
      hasRuntimeStructure: visibleContentBlocks.length > 0,
      visibleRuntimeBlockCount: visibleContentBlocks.length,
      hasRuntimeError: visibleRuntimeError || forbiddenPatterns.some((pattern) => contentText.includes(pattern)),
      hasHorizontalOverflow: document.documentElement.scrollWidth > window.innerWidth + 2,
    };
  }, expectedText);
}

async function sampleMenus(menuTree) {
  const samples = collectMenuSamples(menuTree);
  const records = [];
  for (const sample of samples) {
    for (const child of sample.children) {
      const beforeFailedResponses = failedResponses.length;
      const screenshotName = \`sample-\${sanitizeFileName(sample.topMenu.menuName)}-\${sanitizeFileName(child.menuName || child.menuCode)}.png\`;
      await navigateToMenu(child);
      const expectedContentText = expectedContentTextForMenu(child);
      await waitForRuntimeContentReady(expectedContentText);
      const state = await getRuntimeContentState(expectedContentText);
      await page.screenshot({ path: join(evidenceRoot, screenshotName), fullPage: true });
      const newFailedResponses = failedResponses.slice(beforeFailedResponses)
        .filter((item) => !item.url.endsWith('/favicon.ico'));
      records.push({
        topMenuName: sample.topMenu.menuName,
        topMenuCode: sample.topMenu.menuCode,
        menuName: child.menuName,
        menuCode: child.menuCode,
        path: child.path,
        component: child.component,
        expectedContentText,
        screenshot: screenshotName,
        state,
        newFailedResponses,
        passed: state.hasLayout
          && state.hasAside
          && state.hasMain
          && state.hasRuntimeContent
          && state.runtimeChildCount > 0
          && state.runtimeTextLength > 0
          && state.hasExpectedContentText
          && state.hasRuntimeStructure
          && !state.hasRuntimeError
          && !state.hasHorizontalOverflow
          && newFailedResponses.length === 0,
      });
    }
  }
  const report = {
    rule: 'Every visible top-level menu samples 1-3 child pages; each sampled page validates layout, navigation, overflow, screenshot, and failed API responses.',
    sampleCount: records.length,
    topMenuCount: samples.length,
    records,
  };
  writeFileSync(join(evidenceRoot, 'menu-sampling-report.json'), \`\${JSON.stringify(report, null, 2)}\\n\`, 'utf8');
  const failed = records.filter((record) => !record.passed);
  if (failed.length > 0) {
    throw new Error(\`Menu sampling failed: \${JSON.stringify(failed)}\`);
  }
  return report;
}

async function sampleTopLevelFunctions() {
  const records = [];
  const actions = [
    {
      name: 'user-dropdown',
      expectedText: '个人中心',
      run: async () => {
        await page.locator('.layout-breadcrumb-user').click();
        await waitForVisibleText('.el-dropdown__popper', '个人中心');
      },
      close: closeUserDropdown,
    },
    {
      name: 'settings-drawer',
      expectedText: '布局配置',
      run: async () => {
        await page.locator('.layout-breadcrumb-settings').click();
        await waitForVisibleText('.el-drawer', '布局配置');
        await waitForDrawerFullyVisible('布局配置');
      },
      close: async () => {
        await page.keyboard.press('Escape');
        await page.mouse.click(400, 400);
        await waitForOverlayClosed();
      },
    },
  ];
  for (const action of actions) {
    await action.run();
    const screenshot = \`function-\${action.name}.png\`;
    await page.screenshot({ path: join(evidenceRoot, screenshot), fullPage: true });
    const state = await page.evaluate((expectedText) => ({
      expectedTextVisible: document.body.innerText.includes(expectedText),
      hasHorizontalOverflow: document.documentElement.scrollWidth > window.innerWidth + 2,
    }), action.expectedText);
    records.push({
      functionName: action.name,
      expectedText: action.expectedText,
      screenshot,
      state,
      passed: state.expectedTextVisible && !state.hasHorizontalOverflow,
    });
    await action.close();
  }
  const report = {
    rule: 'Top-level shell functions sample 1-3 subfunctions/actions with screenshots.',
    records,
  };
  writeFileSync(join(evidenceRoot, 'function-sampling-report.json'), \`\${JSON.stringify(report, null, 2)}\\n\`, 'utf8');
  const failed = records.filter((record) => !record.passed);
  if (failed.length > 0) {
    throw new Error(\`Function sampling failed: \${JSON.stringify(failed)}\`);
  }
  return report;
}

async function elementReport(selector) {
  return page.evaluate((targetSelector) => {
    const element = document.querySelector(targetSelector);
    if (!element) return { selector: targetSelector, exists: false, visible: false, rect: null, style: null, text: '' };
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
  const mergeReport = await page.evaluate(() => window.__MANGO_ADMIN_FULL_MENU_MERGE_REPORT__ || null);
  writeFileSync(join(evidenceRoot, 'menu-merge-report.json'), \`\${JSON.stringify(mergeReport, null, 2)}\\n\`, 'utf8');
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
  };
  const horizontalOverflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 2);
  const requiredMenuNames = ['系统管理', '权限管理', '日志管理', '审批中心', '流程办理', '平台能力', '日历管理', '编号规则', '文件管理', '模板管理'];
  const checks = {
    usesFullPresetPackage: await page.evaluate(() => window.__MANGO_ADMIN_FULL_PRESET_E2E__ === 'createMangoAdmin:@mango/admin'),
    hasLayout: elements.layout.visible,
    hasNavbars: elements.navbars.visible && elements.navbars.rect && elements.navbars.rect.height >= 48,
    hasAside: elements.aside.visible && elements.aside.rect && elements.aside.rect.width >= 180,
    hasMain: elements.main.visible,
    hasTags: elements.tags.visible,
    hasUserArea: elements.user.visible,
    hasSettingsEntry: elements.settings.visible,
    hasPrimaryColor: css.primaryColor === '#2E5CF6',
    hasNoBrokenCssLinks: css.brokenLinks.length === 0,
    hasNoHorizontalOverflow: !horizontalOverflow,
    hasBackendMenuTree: menuTree.length > 0,
    hasMenuMergeReport: Boolean(mergeReport),
    keepsBackendMenusAuthoritative: Array.isArray(mergeReport?.items)
      && mergeReport.items.some((item) => item.source === 'backend' && item.action === 'added'),
    hasRequiredMenus: requiredMenuNames.every((name) => menuNames.includes(name)),
    hasHomeText: bodyText.includes('首页'),
  };
  return {
    url: page.url(),
    viewport: await page.viewportSize(),
    checks,
    requiredMenuNames,
    mergeReport,
    menuNames,
    menuTree,
    elements,
    css,
    browser: { consoleErrors, failedResponses, apiResponses },
  };
}

await login();
await waitForHomeReady();
await assertScreenshotState('home', '用户总数', { forbiddenText: '登录成功', forbidDropdown: true, forbidDrawer: true });
await page.screenshot({ path: join(evidenceRoot, 'home-1440x960.png'), fullPage: true });
await page.locator('.layout-breadcrumb-user').click();
await waitForVisibleText('.el-dropdown__popper', '个人中心');
await assertScreenshotState('user-dropdown', '个人中心', { requireDropdown: true, forbidDrawer: true });
await page.screenshot({ path: join(evidenceRoot, 'user-dropdown-1440x960.png'), fullPage: true });
await closeUserDropdown();
await waitForOverlayClosed();
await page.locator('.layout-breadcrumb-settings').click();
await waitForVisibleText('.el-drawer', '布局配置');
await waitForDrawerFullyVisible('布局配置');
await assertScreenshotState('settings-drawer', '布局配置', { forbiddenText: '个人中心', requireDrawer: true, requireDrawerFullyVisible: true, forbidDropdown: true });
await page.screenshot({ path: join(evidenceRoot, 'settings-drawer-1440x960.png'), fullPage: true });
await page.keyboard.press('Escape');
await page.mouse.click(400, 400);
await waitForOverlayClosed();

const menuTreeForSampling = menuResponses.at(-1)?.data || [];
const menuSamplingReport = await sampleMenus(menuTreeForSampling);
await page.goto(\`\${baseUrl}/#/home\`, { waitUntil: 'domcontentloaded' });
await waitForHomeReady();
const functionSamplingReport = await sampleTopLevelFunctions();

const report = await collectReport();
report.menuSampling = menuSamplingReport;
report.functionSampling = functionSamplingReport;
const failedChecks = Object.entries(report.checks).filter(([, passed]) => !passed).map(([name]) => name);
if (failedChecks.length > 0) {
  throw new Error(\`Admin full preset checks failed: \${failedChecks.join(', ')}. Report: \${JSON.stringify(report)}\`);
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
    '# Mango Admin Full Preset E2E',
    '',
    `- Base URL: ${baseUrl}`,
    `- Backend URL: ${args.backendUrl}`,
    '- Entry: temporary app importing `createMangoAdmin` from staged `@mango/admin` file package',
    '- Checks: login, backend menu tree, menu merge report, shell layout, top bar, user area, settings entry, tags, theme color, CSS links, screenshots, menu/page sampling, function sampling',
    '- Evidence: home-1440x960.png, user-dropdown-1440x960.png, settings-drawer-1440x960.png, menu-merge-report.json, menu-sampling-report.json, function-sampling-report.json, layout-report.json',
    '',
  ].join('\n');
  writeFileSync(join(evidenceRoot, 'summary.md'), summary, 'utf8');
}

function run(command, commandArgs, options = {}) {
  log(`$ ${command} ${commandArgs.join(' ')}`);
  const stdio = options.captureFile ? ['ignore', 'pipe', 'pipe'] : 'inherit';
  const result = spawnSync(command, commandArgs, {
    cwd: options.cwd || repoRoot,
    env: options.env || process.env,
    stdio,
    encoding: 'utf8',
  });
  if (options.captureFile) {
    writeFileSync(options.captureFile, `${result.stdout || ''}${result.stderr || ''}`, 'utf8');
  }
  if (result.status !== 0) {
    fail(`Command failed: ${command} ${commandArgs.join(' ')}`);
  }
  return result;
}

function waitForHttp(url) {
  const deadline = Date.now() + 120_000;
  while (Date.now() < deadline) {
    const result = spawnSync('curl', ['-fsS', url], { stdio: 'ignore' });
    if (result.status === 0) return;
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
  if (!child || child.killed) return;
  child.kill('SIGTERM');
}

function resolvePlaywrightModulePath() {
  const directPath = join(repoRoot, 'node_modules/.pnpm/playwright@1.59.1/node_modules/playwright');
  if (existsSync(directPath)) return directPath;
  return dirname(require.resolve('playwright/package.json'));
}

function ensurePlaywrightAvailable() {
  if (!existsSync(playwrightModulePath)) {
    fail(`Playwright module not found: ${playwrightModulePath}`);
  }
}

function log(message) {
  console.log(`[admin-full-preset-e2e] ${message}`);
}

function fail(message) {
  throw new Error(message);
}
