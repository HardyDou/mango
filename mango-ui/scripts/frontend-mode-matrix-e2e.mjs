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
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const scriptFile = fileURLToPath(import.meta.url);
const repoRoot = resolve(dirname(scriptFile), '..');
const packagesRoot = join(repoRoot, 'packages');
const createMangoAppCli = join(packagesRoot, 'create-mango-app/src/index.mjs');
const templateRoot = join(repoRoot, 'packages/create-mango-app/templates/mango-business-starter');
const playwrightModulePath = join(repoRoot, 'node_modules/.pnpm/playwright@1.59.1/node_modules/playwright');
const modes = ['local', 'micro', 'mixed'];
const args = parseArgs(process.argv.slice(2));
const tempRoot = args.keepTemp
  ? mkdirAndReturn(resolve(tmpdir(), `mango-mode-matrix-e2e-${Date.now()}`))
  : mkdtempSync(join(tmpdir(), 'mango-mode-matrix-e2e-'));
const evidenceRoot = args.evidenceDir ? resolve(args.evidenceDir) : join(tempRoot, 'evidence');
const stagedPackageRoot = join(tempRoot, 'staged-packages');
const modeReports = [];
const devServers = [];

try {
  mkdirSync(evidenceRoot, { recursive: true });
  ensurePlaywrightAvailable();
  log(`Temporary root: ${tempRoot}`);
  log(`Evidence root: ${evidenceRoot}`);

  run('pnpm', ['package:build'], { cwd: repoRoot });
  const stagedPackages = stageLocalMangoPackages();

  for (let index = 0; index < modes.length; index += 1) {
    const mode = modes[index];
    const projectName = `mode-${mode}-platform`;
    const projectRoot = join(tempRoot, projectName);
    const modeEvidenceRoot = join(evidenceRoot, mode);
    mkdirSync(modeEvidenceRoot, { recursive: true });
    generateProject(projectName, mode);
    if (mode !== 'local') {
      createRemoteMicroApp(projectRoot, projectName);
    }
    rewriteWorkspaceDependencies(projectRoot, stagedPackages);

    run('node', ['scripts/check-template.mjs'], { cwd: projectRoot });
    run('pnpm', ['install', '--ignore-scripts'], {
      cwd: projectRoot,
      captureFile: join(modeEvidenceRoot, 'install.out'),
    });
    run('pnpm', ['typecheck'], {
      cwd: projectRoot,
      captureFile: join(modeEvidenceRoot, 'typecheck.out'),
    });
    run('pnpm', ['build'], {
      cwd: projectRoot,
      captureFile: join(modeEvidenceRoot, 'build.out'),
    });

    const report = runModeBrowserSmoke({
      mode,
      projectName,
      projectRoot,
      modeEvidenceRoot,
      frontendPort: args.frontendPort + index,
      remotePort: 5190,
    });
    modeReports.push(report);
  }

  writeSummary();
  log('Frontend mode matrix E2E passed.');
} finally {
  for (const child of devServers.reverse()) {
    stopProcess(child);
  }
  if (!args.keepTemp) {
    rmSync(tempRoot, { recursive: true, force: true });
  } else {
    log(`Kept temporary root: ${tempRoot}`);
  }
}

function parseArgs(argv) {
  const parsed = {
    frontendPort: 5290,
    keepTemp: false,
    evidenceDir: '',
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--') {
      continue;
    }
    if (arg === '--keep-temp') {
      parsed.keepTemp = true;
      continue;
    }
    if (arg === '--frontend-port') {
      parsed.frontendPort = Number(argv[index + 1]);
      index += 1;
      continue;
    }
    if (arg === '--evidence-dir') {
      parsed.evidenceDir = argv[index + 1] || '';
      index += 1;
      continue;
    }
    fail(`Unknown argument: ${arg}`);
  }
  if (!Number.isInteger(parsed.frontendPort) || parsed.frontendPort <= 0) {
    fail(`Invalid frontend port: ${parsed.frontendPort}`);
  }
  return parsed;
}

function mkdirAndReturn(path) {
  mkdirSync(path, { recursive: true });
  return path;
}

function generateProject(projectName, mode) {
  run(process.execPath, [
    createMangoAppCli,
    'init',
    projectName,
    '--module',
    'guarantee',
    '--aggregate',
    'letter',
    '--package',
    `com.example.${mode}.guarantee`,
    '--group-id',
    `com.example.${mode}`,
    '--topology',
    'monolith',
    '--features',
    'base,system,rbac,workflow,notice,file',
    '--frontend-mode',
    mode,
    '--version',
    '1.0.0',
    '--template',
    templateRoot,
  ], { cwd: tempRoot });
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

  for (const { packageJson, distDir, targetDir } of packages) {
    rmSync(targetDir, { recursive: true, force: true });
    mkdirSync(targetDir, { recursive: true });
    cpSync(distDir, join(targetDir, 'dist'), { recursive: true });
    const stagedPackageJson = { ...packageJson };
    delete stagedPackageJson.scripts;
    delete stagedPackageJson.devDependencies;
    for (const section of ['dependencies', 'peerDependencies', 'optionalDependencies']) {
      if (!stagedPackageJson[section]) {
        continue;
      }
      for (const dependencyName of Object.keys(stagedPackageJson[section])) {
        if (staged.has(dependencyName)) {
          stagedPackageJson[section][dependencyName] = `file:${staged.get(dependencyName)}`;
        }
      }
    }
    writeFileSync(join(targetDir, 'package.json'), `${JSON.stringify(stagedPackageJson, null, 2)}\n`, 'utf8');
  }

  if (staged.size === 0) {
    fail('No built Mango packages found. Run pnpm package:build first.');
  }
  return staged;
}

function rewriteWorkspaceDependencies(projectRoot, stagedPackages) {
  const packageFiles = findPackageJsonFiles(join(projectRoot, 'frontend'));
  for (const file of packageFiles) {
    const pkg = JSON.parse(readFileSync(file, 'utf8'));
    for (const section of ['dependencies', 'devDependencies', 'peerDependencies']) {
      if (!pkg[section]) {
        continue;
      }
      for (const dependencyName of Object.keys(pkg[section])) {
        if (stagedPackages.has(dependencyName)) {
          pkg[section][dependencyName] = `file:${stagedPackages.get(dependencyName)}`;
        }
      }
    }
    writeFileSync(file, `${JSON.stringify(pkg, null, 2)}\n`, 'utf8');
  }
}

function createRemoteMicroApp(projectRoot, projectName) {
  const remoteRoot = join(projectRoot, 'frontend/apps', `${projectName}-remote`);
  mkdirSync(join(remoteRoot, 'src'), { recursive: true });
  mkdirSync(join(remoteRoot, 'public'), { recursive: true });
  writeFileSync(join(remoteRoot, 'package.json'), `${JSON.stringify({
    name: `${projectName}-remote`,
    version: '1.0.0',
    private: true,
    type: 'module',
    scripts: {
      dev: 'vite --host 127.0.0.1 --port 5190',
      typecheck: 'vue-tsc --noEmit',
      build: 'vite build',
    },
    dependencies: {
      '@vitejs/plugin-vue': '^5.2.1',
      '@vue/tsconfig': '^0.5.1',
      typescript: '^5.9.3',
      vite: '^5.4.21',
      vue: '^3.5.25',
      'vue-tsc': '^2.2.12',
    },
    devDependencies: {},
  }, null, 2)}\n`, 'utf8');
  writeFileSync(join(remoteRoot, 'index.html'), '<!doctype html><html><head><meta charset="UTF-8" /><title>Mango Remote</title></head><body><div id="app"></div><script type="module" src="/src/main.ts"></script></body></html>\n', 'utf8');
  writeFileSync(join(remoteRoot, 'vite.config.ts'), `
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '127.0.0.1',
    port: 5190,
    cors: true,
    headers: {
      'Access-Control-Allow-Origin': '*',
    },
  },
});
`, 'utf8');
  writeFileSync(join(remoteRoot, 'public/health.json'), `${JSON.stringify({
    status: 'UP',
    appCode: `${projectName}-guarantee`,
    version: '1.0.0',
  }, null, 2)}\n`, 'utf8');
  writeFileSync(join(remoteRoot, 'tsconfig.json'), `${JSON.stringify({
    extends: '@vue/tsconfig/tsconfig.dom.json',
    compilerOptions: {
      target: 'ES2020',
      module: 'ESNext',
      moduleResolution: 'Bundler',
      strict: true,
      skipLibCheck: true,
      types: ['vite/client'],
    },
    include: ['src/**/*.ts', 'src/**/*.vue'],
  }, null, 2)}\n`, 'utf8');
  writeFileSync(join(remoteRoot, 'src/main.ts'), createRemoteMicroMain(), 'utf8');
  writeFileSync(join(remoteRoot, 'src/style.css'), createRemoteMicroStyle(), 'utf8');
}

function createRemoteMicroMain() {
  return `
import './style.css';
import { computed, createApp, h, ref, type App } from 'vue';

type MangoRuntime = {
  appCode: string;
  apiBaseUrl?: string;
  menu?: {
    menuName?: string;
    menuCode?: string;
    path?: string;
    component?: string;
    moduleCode?: string;
  };
  permissions?: string[];
  theme?: {
    primary?: string;
    isDark?: boolean;
    tokens?: Record<string, string>;
  };
};

type MangoConfig = {
  appCode: string;
  appName: string;
  entryUrl?: string;
  version?: string;
  healthCheckUrl?: string;
};

declare global {
  interface Window {
    __POWERED_BY_WUJIE__?: boolean;
    __WUJIE_MOUNT?: () => void;
    __WUJIE_UNMOUNT?: () => void;
    $wujie?: {
      props?: {
        mangoRuntime?: MangoRuntime;
        mangoConfig?: MangoConfig;
      };
    };
  }
}

let app: App<Element> | undefined;

const RemoteRoot = {
  setup() {
    const runtime = ref<MangoRuntime>(window.$wujie?.props?.mangoRuntime || {
      appCode: 'standalone',
      apiBaseUrl: '/api',
      menu: {
        menuName: 'Letter管理',
        menuCode: 'guarantee-letter',
        path: '/guarantee/letters',
        component: 'guarantee/letter/index',
        moduleCode: 'guarantee',
      },
      permissions: ['guarantee:letter:list', 'guarantee:letter:create'],
      theme: {
        primary: '#2e5cf6',
      },
    });
    const config = ref<MangoConfig>(window.$wujie?.props?.mangoConfig || {
      appCode: 'standalone',
      appName: 'Standalone Remote',
      entryUrl: window.location.origin,
      version: 'standalone',
      healthCheckUrl: new URL('/health.json', window.location.origin).toString(),
    });
    const primary = computed(() => runtime.value.theme?.primary || '#2e5cf6');
    return () => h('section', {
      class: 'mango-mode-remote-page',
      'data-mango-layout': 'remote-page',
      style: {
        '--remote-primary': primary.value,
      },
    }, [
      h('header', { class: 'remote-page-header' }, [
        h('div', [
          h('p', { class: 'remote-page-kicker' }, 'Mango Remote Runtime'),
          h('h1', 'Letter管理'),
        ]),
        h('span', { class: 'remote-version', 'data-testid': 'remote-version' }, config.value.version || 'unknown'),
      ]),
      h('div', { class: 'remote-grid' }, [
        h('article', { class: 'remote-panel', 'data-testid': 'remote-menu' }, [
          h('strong', '菜单'),
          h('span', runtime.value.menu?.menuName || '-'),
          h('small', runtime.value.menu?.component || '-'),
        ]),
        h('article', { class: 'remote-panel', 'data-testid': 'remote-permissions' }, [
          h('strong', '权限'),
          h('span', String(runtime.value.permissions?.length || 0)),
          h('small', (runtime.value.permissions || []).slice(0, 3).join(', ') || '-'),
        ]),
        h('article', { class: 'remote-panel', 'data-testid': 'remote-theme' }, [
          h('strong', '主题'),
          h('span', primary.value),
          h('small', runtime.value.theme?.isDark ? 'dark' : 'light'),
        ]),
        h('article', { class: 'remote-panel', 'data-testid': 'remote-health' }, [
          h('strong', '健康检查'),
          h('span', 'UP'),
          h('small', config.value.healthCheckUrl || '-'),
        ]),
      ]),
      h('div', { class: 'remote-table', 'data-mango-layout': 'remote-table' }, [
        h('div', { class: 'remote-table-row remote-table-head' }, [
          h('span', '业务标识'),
          h('span', '业务名称'),
          h('span', '运行单元'),
        ]),
        h('div', { class: 'remote-table-row' }, [
          h('span', 'LETTER-REMOTE-001'),
          h('span', '远程保函业务'),
          h('span', config.value.appCode),
        ]),
      ]),
    ]);
  },
};

function mount() {
  const root = document.querySelector('#app');
  if (!root) {
    return;
  }
  app = createApp(RemoteRoot);
  app.mount(root);
}

function unmount() {
  app?.unmount();
  app = undefined;
}

if (window.__POWERED_BY_WUJIE__) {
  window.__WUJIE_MOUNT = mount;
  window.__WUJIE_UNMOUNT = unmount;
} else {
  mount();
}
`;
}

function createRemoteMicroStyle() {
  return `
:root {
  color: #1f2d3d;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

body {
  margin: 0;
}

.mango-mode-remote-page {
  min-height: 360px;
  padding: 16px;
  background: #f5f7fb;
  color: #1f2d3d;
}

.remote-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
  padding: 14px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #ffffff;
}

.remote-page-header h1 {
  margin: 2px 0 0;
  color: #1f2d3d;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.4;
}

.remote-page-kicker {
  margin: 0;
  color: var(--remote-primary);
  font-size: 12px;
  font-weight: 600;
}

.remote-version {
  display: inline-flex;
  align-items: center;
  min-width: 64px;
  height: 26px;
  padding: 0 10px;
  border-radius: 4px;
  background: var(--remote-primary);
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
}

.remote-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.remote-panel {
  min-height: 86px;
  padding: 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #ffffff;
}

.remote-panel strong,
.remote-panel span,
.remote-panel small {
  display: block;
}

.remote-panel strong {
  margin-bottom: 8px;
  color: #606266;
  font-size: 13px;
}

.remote-panel span {
  color: #1f2d3d;
  font-size: 18px;
  font-weight: 600;
}

.remote-panel small {
  overflow: hidden;
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remote-table {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #ffffff;
}

.remote-table-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1.6fr;
  min-height: 42px;
  border-top: 1px solid #ebeef5;
}

.remote-table-row:first-child {
  border-top: 0;
}

.remote-table-row span {
  display: flex;
  align-items: center;
  min-width: 0;
  padding: 0 12px;
  overflow: hidden;
  color: #303133;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remote-table-head {
  background: #f5f7fa;
  font-weight: 600;
}
`;
}

function findPackageJsonFiles(root) {
  const files = [];
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    const path = join(root, entry.name);
    if (entry.isDirectory()) {
      files.push(...findPackageJsonFiles(path));
      continue;
    }
    if (entry.name === 'package.json') {
      files.push(path);
    }
  }
  return files;
}

function runModeBrowserSmoke({ mode, projectName, projectRoot, modeEvidenceRoot, frontendPort, remotePort }) {
  const appName = `${projectName}-admin`;
  if (mode !== 'local') {
    const remoteAppName = `${projectName}-remote`;
    const remoteServer = spawn('pnpm', [
      '--filter',
      remoteAppName,
      'dev',
      '--host',
      '127.0.0.1',
      '--port',
      String(remotePort),
    ], {
      cwd: projectRoot,
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    devServers.push(remoteServer);
    pipeToFile(remoteServer.stdout, join(modeEvidenceRoot, 'remote-dev.out'));
    pipeToFile(remoteServer.stderr, join(modeEvidenceRoot, 'remote-dev.err'));
    waitForHttp(`http://127.0.0.1:${remotePort}/health.json`);
  }
  const devServer = spawn('pnpm', [
    '--filter',
    appName,
    'dev',
    '--host',
    '127.0.0.1',
    '--port',
    String(frontendPort),
  ], {
    cwd: projectRoot,
    stdio: ['ignore', 'pipe', 'pipe'],
    env: {
      ...process.env,
      VITE_MANGO_E2E: 'true',
    },
  });
  devServers.push(devServer);
  pipeToFile(devServer.stdout, join(modeEvidenceRoot, 'frontend-dev.out'));
  pipeToFile(devServer.stderr, join(modeEvidenceRoot, 'frontend-dev.err'));
  waitForHttp(`http://127.0.0.1:${frontendPort}/`);

  const smokeFile = join(tempRoot, `${mode}-frontend-smoke.mjs`);
  writeFileSync(smokeFile, createFrontendSmokeScript(), 'utf8');
  const reportFile = join(modeEvidenceRoot, 'layout-report.json');
  const screenshotFile = join(modeEvidenceRoot, 'frontend-mode.png');
  const failureReportFile = join(modeEvidenceRoot, 'remote-failure-report.json');
  const failureScreenshotFile = join(modeEvidenceRoot, 'remote-failure.png');
  run(process.execPath, [smokeFile], {
    cwd: projectRoot,
    env: {
      ...process.env,
      MANGO_MODE_MATRIX_URL: `http://127.0.0.1:${frontendPort}/`,
      MANGO_MODE_MATRIX_MODE: mode,
      MANGO_MODE_MATRIX_SCREENSHOT: screenshotFile,
      MANGO_MODE_MATRIX_REPORT: reportFile,
      MANGO_MODE_MATRIX_FAILURE_SCREENSHOT: failureScreenshotFile,
      MANGO_MODE_MATRIX_FAILURE_REPORT: failureReportFile,
      MANGO_PLAYWRIGHT_MODULE: playwrightModulePath,
    },
  });
  return JSON.parse(readFileSync(reportFile, 'utf8'));
}

function createFrontendSmokeScript() {
  return `
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.MANGO_PLAYWRIGHT_MODULE);
const baseUrl = process.env.MANGO_MODE_MATRIX_URL;
const mode = process.env.MANGO_MODE_MATRIX_MODE;
const screenshot = process.env.MANGO_MODE_MATRIX_SCREENSHOT;
const reportFile = process.env.MANGO_MODE_MATRIX_REPORT;
const failureScreenshot = process.env.MANGO_MODE_MATRIX_FAILURE_SCREENSHOT;
const failureReportFile = process.env.MANGO_MODE_MATRIX_FAILURE_REPORT;
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1366, height: 900 } });
const consoleErrors = [];
const failedResponses = [];
page.on('console', (message) => {
  if (message.type() === 'error') {
    consoleErrors.push(message.text());
  }
});
page.on('pageerror', (error) => {
  consoleErrors.push(error.message);
});
page.on('response', (response) => {
  if (response.status() >= 400) {
    failedResponses.push({ status: response.status(), url: response.url() });
  }
});

await page.goto(baseUrl, { waitUntil: 'networkidle' });
await page.waitForSelector('.layout-container', { timeout: 15000 });
await page.goto(\`\${baseUrl}#/guarantee/letters\`, { waitUntil: 'networkidle' });
await page.waitForSelector('.shell-runtime-content', { timeout: 15000 });
await page.waitForFunction(() => document.body.innerText.includes('Letter管理'), undefined, { timeout: 15000 });

if (mode === 'local') {
  await page.waitForSelector('[data-mango-layout="list-page"]', { timeout: 15000 });
} else {
  await page.waitForFunction(() => {
    const content = document.querySelector('.shell-runtime-content');
    return content?.getAttribute('data-mango-runtime-page-type') === 'MICRO_ROUTE';
  }, undefined, { timeout: 15000 });
  await page.waitForFunction(() => {
    return document.querySelector('[data-mango-layout="remote-page"]')
      || Array.from(document.querySelectorAll('iframe')).some((frame) => {
        try {
          return frame.contentDocument?.querySelector('[data-mango-layout="remote-page"]');
        } catch {
          return false;
        }
      });
  }, undefined, { timeout: 30000 });
}

const report = await page.evaluate((mode) => {
  function elementReport(selector, root = document) {
    const element = root.querySelector(selector);
    if (!element) {
      return { selector, exists: false, visible: false, rect: null, style: null, text: '' };
    }
    const style = window.getComputedStyle(element);
    const rect = element.getBoundingClientRect();
    return {
      selector,
      exists: true,
      visible: style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0,
      text: (element.textContent || '').trim().replace(/\\s+/g, ' ').slice(0, 160),
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
  }
  function remoteDocument() {
    if (document.querySelector('[data-mango-layout="remote-page"]')) {
      return document;
    }
    for (const frame of Array.from(document.querySelectorAll('iframe'))) {
      try {
        if (frame.contentDocument?.querySelector('[data-mango-layout="remote-page"]')) {
          return frame.contentDocument;
        }
      } catch {
        // Cross-origin frames are reported through shell/runtime checks only.
      }
    }
    return document;
  }
  function styleSheetReport() {
    return Array.from(document.styleSheets).map((sheet) => {
      try {
        return { href: sheet.href, rules: sheet.cssRules?.length ?? null };
      } catch {
        return { href: sheet.href, inaccessible: true };
      }
    });
  }
  function isTransparent(color) {
    return color === 'transparent' || color === 'rgba(0, 0, 0, 0)';
  }
  const runtime = elementReport('.shell-runtime-content');
  const shell = elementReport('.layout-container');
  const header = elementReport('.layout-header');
  const nav = elementReport('.layout-navbars-container');
  const aside = elementReport('.layout-aside');
  const main = elementReport('.layout-main');
  const topSystemButton = elementReport('.layout-top-system-item');
  const listPage = elementReport('[data-mango-layout="list-page"]');
  const search = elementReport('[data-mango-layout="search"]');
  const actions = elementReport('[data-mango-layout="actions"]');
  const table = elementReport('[data-mango-layout="table"]');
  const pagination = elementReport('[data-mango-layout="pagination"]');
  const runtimeEmpty = elementReport('.micro-runtime-empty');
  const runtimeRetry = elementReport('.micro-runtime-retry');
  const remoteRoot = remoteDocument();
  const remotePage = elementReport('[data-mango-layout="remote-page"]', remoteRoot);
  const remoteTable = elementReport('[data-mango-layout="remote-table"]', remoteRoot);
  const remoteMenu = elementReport('[data-testid="remote-menu"]', remoteRoot);
  const remotePermissions = elementReport('[data-testid="remote-permissions"]', remoteRoot);
  const remoteTheme = elementReport('[data-testid="remote-theme"]', remoteRoot);
  const remoteHealth = elementReport('[data-testid="remote-health"]', remoteRoot);
  const remoteVersion = elementReport('[data-testid="remote-version"]', remoteRoot);
  const bodyText = document.body.innerText;
  const remoteText = remoteRoot === document ? bodyText : remoteRoot.body?.innerText || '';
  const styleSheets = styleSheetReport();
  const brokenCssLinks = styleSheets.filter((item) => item.href && item.rules === 0);
  const runtimeElement = document.querySelector('.shell-runtime-content');
  const runtimeDecision = {
    pageType: runtimeElement?.getAttribute('data-mango-runtime-page-type') || '',
    mode: runtimeElement?.getAttribute('data-mango-runtime-mode') || '',
    moduleCode: runtimeElement?.getAttribute('data-mango-runtime-module') || '',
    runtimeCode: runtimeElement?.getAttribute('data-mango-runtime-code') || '',
    entry: runtimeElement?.getAttribute('data-mango-runtime-entry') || '',
    version: runtimeElement?.getAttribute('data-mango-runtime-version') || '',
    healthCheckUrl: runtimeElement?.getAttribute('data-mango-runtime-health-check') || '',
  };
  const horizontalOverflow = document.documentElement.scrollWidth > window.innerWidth + 2;
  const shellChecks = {
    hasShellLayout: shell.visible,
    hasHeaderLayout: header.visible && header.rect.height >= 48 && header.rect.height <= 72,
    hasNavFlexLayout: nav.visible && nav.style.display === 'flex' && nav.rect.width >= 900,
    hasAsideLayout: aside.visible && aside.rect.width >= 200 && aside.rect.width <= 240,
    hasMainLayout: main.visible && main.rect.x >= aside.rect.width,
    hasStyledTopButton: !topSystemButton.exists || (
      topSystemButton.style.display.includes('flex')
      && topSystemButton.style.border.startsWith('0px')
      && !isTransparent(topSystemButton.style.color)
    ),
    hasNoBrokenCssLinks: brokenCssLinks.length === 0,
    hasAdminShellStyles: styleSheets.some((item) => item.href && item.href.includes('@mango_admin-shell_style') && (item.rules || 0) > 20)
      || styleSheets.some((item) => item.href && item.href.includes('@mango/admin-shell') && (item.rules || 0) > 20)
      || styleSheets.some((item) => !item.href && (item.rules || 0) > 200),
  };
  const localChecks = {
    hasListPage: listPage.visible,
    hasSearch: search.visible,
    hasActions: actions.visible,
    hasTable: table.visible,
    hasPagination: pagination.visible,
    hasExplicitApiErrorState: bodyText.includes('加载Letter列表失败')
      || bodyText.includes('Internal Server Error')
      || bodyText.includes('500'),
    verticalOrder: Boolean(search.rect && actions.rect && table.rect && pagination.rect
      && search.rect.bottom <= actions.rect.top
      && actions.rect.bottom <= table.rect.top
      && table.rect.bottom <= pagination.rect.top),
  };
  const microChecks = {
    hasRuntimeOutlet: runtime.visible,
    hasMicroDecision: runtimeDecision.pageType === 'MICRO_ROUTE' && runtimeDecision.mode === 'micro',
    hasRuntimeVersion: runtimeDecision.version === '1.0.0',
    hasRuntimeHealthCheck: runtimeDecision.healthCheckUrl.endsWith('/health.json'),
    hasRemotePage: remotePage.visible,
    hasStyledRemotePage: remotePage.style?.backgroundColor === 'rgb(245, 247, 251)' && remotePage.style?.color === 'rgb(31, 45, 61)',
    hasRemoteMenu: remoteMenu.visible && remoteMenu.text.includes('Letter管理') && remoteMenu.text.includes('guarantee/letter/index'),
    hasRemotePermissions: remotePermissions.visible && !remotePermissions.text.includes('权限0') && remotePermissions.text.includes('guarantee:letter:list'),
    hasRemoteTheme: remoteTheme.visible && remoteTheme.text.includes('#2e5cf6'),
    hasRemoteHealth: remoteHealth.visible && remoteHealth.text.includes('UP'),
    hasRemoteVersion: remoteVersion.visible && remoteVersion.text.includes('1.0.0'),
    hasStyledRemoteVersion: remoteVersion.style?.backgroundColor === 'rgb(46, 92, 246)' && remoteVersion.style?.color === 'rgb(255, 255, 255)',
    hasRemoteBusinessData: remoteText.includes('LETTER-REMOTE-001') && remoteText.includes('远程保函业务'),
    hasRemoteTable: remoteTable.visible,
  };
  return {
    mode,
    url: window.location.href,
    viewport: {
      width: window.innerWidth,
      height: window.innerHeight,
    },
    runtimeDecision,
    remoteContext: {
      source: remoteRoot === document ? 'main-document' : 'iframe',
      text: remoteText.trim().replace(/\\s+/g, ' ').slice(0, 240),
    },
    checks: {
      ...shellChecks,
      hasBusinessMenu: bodyText.includes('Letter管理'),
      noHorizontalOverflow: !horizontalOverflow,
      ...(mode === 'local' ? localChecks : microChecks),
    },
    elements: {
      shell,
      header,
      nav,
      aside,
      main,
      topSystemButton,
      runtime,
      listPage,
      search,
      actions,
      table,
      pagination,
      runtimeEmpty,
      runtimeRetry,
      remotePage,
      remoteTable,
      remoteMenu,
      remotePermissions,
      remoteTheme,
      remoteHealth,
      remoteVersion,
    },
    css: {
      brokenLinks: brokenCssLinks,
      styleSheets,
    },
  };
}, mode);

const failedChecks = Object.entries(report.checks)
  .filter(([, passed]) => !passed)
  .map(([name]) => name);
if (failedChecks.length > 0) {
  throw new Error(\`Frontend mode \${mode} layout checks failed: \${failedChecks.join(', ')}. Report: \${JSON.stringify(report)}\`);
}

const unexpectedErrors = consoleErrors.filter((message) => {
  if (message === 'Failed to load resource: the server responded with a status of 404 (Not Found)') {
    return false;
  }
  if (report.checks.hasBusinessMenu && message === 'Failed to load resource: the server responded with a status of 500 (Internal Server Error)') {
    return false;
  }
  if (mode === 'local' && message === 'Failed to load resource: the server responded with a status of 500 (Internal Server Error)') {
    return false;
  }
  if (mode !== 'local') {
    return !message.includes('Mango micro app health check failed')
      && !message.includes('Failed to load Mango micro app')
      && !message.includes('Failed to mount Mango micro app')
      && !message.includes('ERR_CONNECTION_REFUSED');
  }
  return true;
});
if (unexpectedErrors.length > 0) {
  throw new Error(\`Unexpected browser console errors:\\n\${unexpectedErrors.join('\\n')}\`);
}

const unexpectedResponses = failedResponses.filter((item) => {
  const url = new URL(item.url);
  if (url.pathname === '/favicon.ico') {
    return false;
  }
  if (report.checks.hasBusinessMenu && url.pathname === '/api/authorization/menus/user') {
    return false;
  }
  if (mode === 'local' && url.pathname.includes('/api/guarantee/letters')) {
    return false;
  }
  if (mode !== 'local' && url.origin === 'http://127.0.0.1:5190') {
    return false;
  }
  return true;
});
if (unexpectedResponses.length > 0) {
  throw new Error(\`Unexpected failed browser responses:\\n\${unexpectedResponses.map(item => \`\${item.status} \${item.url}\`).join('\\n')}\`);
}

report.browser = {
  consoleErrors,
  failedResponses,
};
await page.screenshot({ path: screenshot, fullPage: true });
if (mode !== 'local') {
  const failurePage = await browser.newPage({ viewport: { width: 1366, height: 900 } });
  await failurePage.route('**/mango-runtime-config.json', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        profile: mode === 'mixed' ? 'hybrid' : 'micro',
        modules: {
          guarantee: {
            mode: 'micro',
            entry: 'http://127.0.0.1:5999/',
            runtimeCode: \`mode-\${mode}-platform-guarantee\`,
            appType: 'MICRO_APP',
            framework: 'vue3',
            version: '1.0.0',
            healthCheckUrl: 'http://127.0.0.1:5999/health.json',
          },
        },
      }),
    });
  });
  await failurePage.goto(baseUrl, { waitUntil: 'networkidle' });
  await failurePage.goto(\`\${baseUrl}#/guarantee/letters\`, { waitUntil: 'networkidle' });
  await failurePage.waitForFunction(() => {
    const text = document.body.innerText;
    return text.includes('页面加载失败') && text.includes('Mango micro app health check failed');
  }, undefined, { timeout: 30000 });
  const failureReport = await failurePage.evaluate(() => {
    function elementReport(selector) {
      const element = document.querySelector(selector);
      if (!element) {
        return { selector, exists: false, visible: false, rect: null, style: null, text: '' };
      }
      const style = window.getComputedStyle(element);
      const rect = element.getBoundingClientRect();
      return {
        selector,
        exists: true,
        visible: style.display !== 'none' && style.visibility !== 'hidden' && rect.width > 0 && rect.height > 0,
        text: (element.textContent || '').trim().replace(/\\s+/g, ' ').slice(0, 300),
        rect: {
          x: Math.round(rect.x),
          y: Math.round(rect.y),
          width: Math.round(rect.width),
          height: Math.round(rect.height),
        },
        style: {
          display: style.display,
          backgroundColor: style.backgroundColor,
          color: style.color,
          border: style.border,
        },
      };
    }
    const runtimeEmpty = elementReport('.micro-runtime-empty');
    const runtimeRetry = elementReport('.micro-runtime-retry');
    const bodyText = document.body.innerText;
    const checks = {
    hasFailureTitle: bodyText.includes('页面加载失败'),
    hasHealthCheckDiagnostic: bodyText.includes('Mango micro app health check failed'),
      hasRuntimeCode: bodyText.includes('运行单元：mode-') && bodyText.includes('platform-guarantee'),
      hasEntryDiagnostic: bodyText.includes('http://127.0.0.1:5999/'),
      hasHealthDiagnostic: bodyText.includes('健康检查：http://127.0.0.1:5999/health.json'),
      hasStyledMicroErrorState: runtimeEmpty.visible
        && runtimeEmpty.style.display === 'flex'
        && runtimeEmpty.style.backgroundColor === 'rgb(255, 255, 255)'
        && runtimeEmpty.style.border.startsWith('1px')
        && runtimeRetry.visible
        && runtimeRetry.style.display.includes('flex')
        && runtimeRetry.style.backgroundColor === 'rgb(46, 92, 246)'
        && runtimeRetry.style.color === 'rgb(255, 255, 255)',
    };
    return { checks, elements: { runtimeEmpty, runtimeRetry }, bodyText: bodyText.slice(0, 1000) };
  });
  const failedFailureChecks = Object.entries(failureReport.checks)
    .filter(([, passed]) => !passed)
    .map(([name]) => name);
  if (failedFailureChecks.length > 0) {
    throw new Error(\`Frontend mode \${mode} remote failure checks failed: \${failedFailureChecks.join(', ')}. Report: \${JSON.stringify(failureReport)}\`);
  }
  await failurePage.screenshot({ path: failureScreenshot, fullPage: true });
  await import('node:fs').then(({ writeFileSync }) => {
    writeFileSync(failureReportFile, \`\${JSON.stringify(failureReport, null, 2)}\\n\`, 'utf8');
  });
  await failurePage.close();
}
await import('node:fs').then(({ writeFileSync }) => {
  writeFileSync(reportFile, \`\${JSON.stringify(report, null, 2)}\\n\`, 'utf8');
});
await browser.close();
`;
}

function writeSummary() {
  const summary = [
    '# Mango Frontend Mode Matrix E2E',
    '',
    `- Temporary root: ${tempRoot}`,
    `- Modes: ${modes.join(', ')}`,
    '- Checks: package build, generated project template check, install, typecheck, build, browser smoke',
    '- UI layout checks:',
    '  - local: business list page, search, actions, table, pagination, vertical order, horizontal overflow',
    '  - micro/mixed: shell runtime outlet, real remote page, runtime version, health check, menu, permissions, theme, business data, horizontal overflow',
    '  - micro/mixed negative: remote health check failure renders Mango standard error panel',
    '',
    '| Mode | Runtime decision | Layout result | Evidence |',
    '|---|---|---|---|',
    ...modeReports.map((report) => {
      const failedChecks = Object.entries(report.checks || {})
        .filter(([, passed]) => !passed)
        .map(([name]) => name);
      const result = failedChecks.length ? `failed: ${failedChecks.join(', ')}` : 'passed';
      const decision = `${report.runtimeDecision?.pageType || ''}/${report.runtimeDecision?.mode || ''}`;
      const failureEvidence = report.mode === 'local' ? '' : `, ${report.mode}/remote-failure-report.json, ${report.mode}/remote-failure.png`;
      return `| ${report.mode} | ${decision} | ${result} | ${report.mode}/layout-report.json, ${report.mode}/frontend-mode.png${failureEvidence} |`;
    }),
    '',
  ].join('\n');
  writeFileSync(join(evidenceRoot, 'summary.md'), summary, 'utf8');
}

function run(command, commandArgs, options = {}) {
  log(`$ ${command} ${commandArgs.join(' ')}`);
  const result = spawnSync(command, commandArgs, {
    cwd: options.cwd || repoRoot,
    env: options.env || process.env,
    stdio: options.capture || options.captureFile ? 'pipe' : 'inherit',
    encoding: 'utf8',
  });
  if (options.captureFile) {
    writeFileSync(options.captureFile, `${result.stdout || ''}${result.stderr || ''}`, 'utf8');
  }
  if (result.status !== 0) {
    if (options.capture || options.captureFile) {
      process.stdout.write(result.stdout || '');
      process.stderr.write(result.stderr || '');
    }
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

function ensurePlaywrightAvailable() {
  if (!existsSync(playwrightModulePath)) {
    fail(`Playwright module not found: ${playwrightModulePath}`);
  }
}

function log(message) {
  console.log(`[mode-matrix-e2e] ${message}`);
}

function fail(message) {
  throw new Error(message);
}
