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
const apiPackageNames = ['auth', 'calendar', 'file', 'notice', 'numgen', 'rbac', 'system', 'template', 'workflow'];
const args = parseArgs(process.argv.slice(2));
const tempRoot = args.keepTemp
  ? mkdirAndReturn(resolve(tmpdir(), `mango-api-sdk-consumer-e2e-${Date.now()}`))
  : mkdtempSync(join(tmpdir(), 'mango-api-sdk-consumer-e2e-'));
const appRoot = join(tempRoot, 'api-sdk-consumer-app');
const stagedPackageRoot = join(tempRoot, 'staged-packages');
const evidenceRoot = resolve(args.evidenceDir || join(workspaceRoot, 'mango-docs/evidence/2026-05-30-sprint-2/api-sdk-consumer'));
const baseUrl = `http://127.0.0.1:${args.frontendPort}`;
let devServerProcess;

try {
  mkdirSync(evidenceRoot, { recursive: true });
  ensurePlaywrightAvailable();
  log(`Temporary root: ${tempRoot}`);
  log(`Evidence root: ${evidenceRoot}`);
  log(`Base URL: ${baseUrl}`);

  if (!args.skipPackageBuild) {
    buildApiSdkPackages();
  }
  const stagedPackages = stageLocalPackages();
  createConsumerApp(stagedPackages);
  run('pnpm', ['install', '--ignore-scripts'], {
    cwd: appRoot,
    captureFile: join(evidenceRoot, 'install.out'),
  });
  assertNoForbiddenConsumerDependencies();
  run('pnpm', ['exec', 'vue-tsc', '--noEmit', '-p', 'tsconfig.json'], {
    cwd: appRoot,
    captureFile: join(evidenceRoot, 'typecheck.out'),
  });
  run('pnpm', ['exec', 'vite', 'build'], {
    cwd: appRoot,
    captureFile: join(evidenceRoot, 'vite-build.out'),
  });

  devServerProcess = startConsumerDevServer();
  waitForHttp(baseUrl);
  runBrowserCheck();
  writeSummary();
  log('API SDK consumer E2E passed.');
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
    frontendPort: 7791,
    evidenceDir: '',
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
    if (arg === '--evidence-dir') {
      parsed.evidenceDir = argv[index + 1] || '';
      index += 1;
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
  return parsed;
}

function mkdirAndReturn(path) {
  mkdirSync(path, { recursive: true });
  return path;
}

function buildApiSdkPackages() {
  for (const packageName of ['@mango/api-schema', '@mango/common', ...apiPackageNames.map(name => `@mango/${name}-api`)]) {
    run('pnpm', ['package:build', '--filter', packageName], { cwd: repoRoot });
  }
}

function stageLocalPackages() {
  mkdirSync(stagedPackageRoot, { recursive: true });
  const packageNames = new Set(['@mango/api-schema', '@mango/common', ...apiPackageNames.map(name => `@mango/${name}-api`)]);
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
    if (!packageNames.has(packageJson.name)) {
      continue;
    }
    const targetDir = join(stagedPackageRoot, packageJson.name.replace('@', '').replace('/', '-'));
    packages.push({ packageJson, distDir, targetDir });
    staged.set(packageJson.name, targetDir);
  }

  for (const packageName of packageNames) {
    if (!staged.has(packageName)) {
      fail(`Built package not found for API SDK consumer: ${packageName}`);
    }
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

function createConsumerApp(stagedPackages) {
  mkdirSync(join(appRoot, 'src'), { recursive: true });
  writeFileSync(join(appRoot, 'index.html'), '<div id="app"></div><script type="module" src="/src/main.ts"></script>\n', 'utf8');
  writeFileSync(join(appRoot, 'src/main.ts'), createMainTs(), 'utf8');
  writeFileSync(join(appRoot, 'src/App.vue'), createAppVue(), 'utf8');
  writeFileSync(join(appRoot, 'vite.config.mjs'), [
    "import { defineConfig } from 'vite';",
    "import vue from '@vitejs/plugin-vue';",
    '',
    'export default defineConfig({',
    '  plugins: [vue()],',
    '});',
    '',
  ].join('\n'), 'utf8');
  writeFileSync(join(appRoot, 'tsconfig.json'), `${JSON.stringify({
    compilerOptions: {
      target: 'ES2020',
      module: 'ESNext',
      moduleResolution: 'bundler',
      strict: true,
      jsx: 'preserve',
      lib: ['ES2020', 'DOM', 'DOM.Iterable'],
      skipLibCheck: true,
      allowSyntheticDefaultImports: true,
      esModuleInterop: true,
      types: ['vite/client'],
    },
    include: ['src/**/*.ts', 'src/**/*.vue'],
  }, null, 2)}\n`, 'utf8');
  writeFileSync(join(appRoot, 'package.json'), `${JSON.stringify({
    name: 'mango-api-sdk-consumer-app',
    private: true,
    type: 'module',
    scripts: {
      build: 'vite build',
      dev: 'vite',
      typecheck: 'vue-tsc --noEmit -p tsconfig.json',
    },
    dependencies: {
      '@vitejs/plugin-vue': '^5.2.1',
      vite: '^5.1.4',
      typescript: '^5.3.3',
      'vue-tsc': '^2.2.12',
      vue: '^3.5.13',
      axios: '^1.3.3',
      qs: '^6.11.0',
      'js-cookie': '^3.0.1',
      nprogress: '^0.2.0',
      'sm-crypto': '^0.3.12',
      '@mango/common': `file:${stagedPackages.get('@mango/common')}`,
      '@mango/api-schema': `file:${stagedPackages.get('@mango/api-schema')}`,
      ...Object.fromEntries(apiPackageNames.map(name => [`@mango/${name}-api`, `file:${stagedPackages.get(`@mango/${name}-api`)}`])),
    },
  }, null, 2)}\n`, 'utf8');
}

function createMainTs() {
  return [
    "import { createApp } from 'vue';",
    "import { setRequestBaseUrl } from '@mango/common';",
    "import App from './App.vue';",
    '',
    "setRequestBaseUrl('/api');",
    "createApp(App).mount('#app');",
    '',
  ].join('\n');
}

function createAppVue() {
  return `<template>
  <main class="api-sdk-page">
    <header>
      <p class="eyebrow">Non-admin UI</p>
      <h1>Mango API SDK Consumer</h1>
      <p class="subtitle">Independent Vite application importing only @mango/*-api packages.</p>
    </header>
    <section class="summary" aria-label="API SDK summary">
      <article v-for="item in packageSummaries" :key="item.name">
        <span>{{ item.name }}</span>
        <strong>{{ item.exports }}</strong>
      </article>
    </section>
    <section class="checks" aria-label="Runtime checks">
      <div data-testid="api-count">API packages: {{ packageSummaries.length }}</div>
      <div data-testid="consumer-boundary">Admin shell imports: 0</div>
      <div data-testid="sample-types">Sample IDs: {{ sampleIds.join(', ') }}</div>
    </section>
  </main>
</template>

<script setup lang="ts">
import type { ApiId } from '@mango/api-schema';
import { login, getUserInfo } from '@mango/auth-api';
import { calendarApi, type CalendarVO } from '@mango/calendar-api';
import { fileApi, defaultFileSettings, type FileRecord } from '@mango/file-api';
import { getMyUnreadCount, type NoticeUnreadCount } from '@mango/notice-api';
import { numgenApi, type NumgenGenerator } from '@mango/numgen-api';
import { menuApi, appApi, type SysMenuVO, type AuthorizationApp } from '@mango/rbac-api';
import { dictTypeApi, tenantApi, type DictType, type SysTenant } from '@mango/system-api';
import { templateApi, type TemplateItem } from '@mango/template-api';
import { workflowApi, type WorkflowDefinition } from '@mango/workflow-api';

const sampleIds: ApiId[] = ['1001', '1002'];
const samples = {
  auth: [login, getUserInfo],
  calendar: calendarApi,
  file: [fileApi, defaultFileSettings],
  notice: getMyUnreadCount,
  numgen: numgenApi,
  rbac: [menuApi, appApi],
  system: [dictTypeApi, tenantApi],
  template: templateApi,
  workflow: workflowApi,
};

const typedExamples: [
  CalendarVO,
  FileRecord,
  NoticeUnreadCount,
  NumgenGenerator,
  SysMenuVO,
  AuthorizationApp,
  DictType,
  SysTenant,
  TemplateItem,
  WorkflowDefinition,
] = [
  { calendarCode: 'WORK', calendarName: 'Work Calendar' },
  { id: 'file-1', fileName: 'contract.pdf', fileSize: 1024 },
  { count: 0 },
  { id: 'gen-1', genKey: 'ORDER', genName: 'Order No' },
  { menuId: 'menu-1', parentId: '0', menuType: 2, menuName: 'Menu', menuCode: 'menu', path: '/menu', sort: 1, status: 1, visible: 1 },
  { appCode: 'biz', appName: 'Business App', status: 1 },
  { id: 'dict-1', name: 'Status', code: 'status' },
  { id: 'tenant-1', tenantName: 'Mango Group', tenantCode: 'mango' },
  { id: 1, templateCode: 'tpl', templateName: 'Template', status: 1, currentVersionNo: 1 },
  { id: 'wf-1', categoryId: 'cat-1', definitionName: 'Flow', definitionKey: 'flow', designerJson: '{}', status: 'DRAFT' },
];

const packageSummaries = Object.entries(samples).map(([name, value]) => ({
  name: \`@mango/\${name}-api\`,
  exports: Array.isArray(value) ? value.length : Object.keys(value as Record<string, unknown>).length,
}));

(window as typeof window & { __MANGO_API_SDK_CONSUMER__: { packageCount: number; sampleCount: number; hasAdminShell: boolean } }).__MANGO_API_SDK_CONSUMER__ = {
  packageCount: packageSummaries.length,
  sampleCount: typedExamples.length,
  hasAdminShell: false,
};
</script>

<style scoped>
.api-sdk-page {
  min-height: 100vh;
  padding: 40px;
  color: #1f2937;
  background: #f7f9fc;
  font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
}

header {
  max-width: 880px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 700;
  color: #2e5cf6;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  font-size: 34px;
  line-height: 1.18;
}

.subtitle {
  margin: 12px 0 0;
  color: #64748b;
}

.summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 980px;
  margin-top: 28px;
}

article,
.checks {
  border: 1px solid #d7deea;
  border-radius: 8px;
  background: #fff;
}

article {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 48px;
  padding: 12px 14px;
}

article span {
  font-size: 14px;
}

article strong {
  color: #0f766e;
}

.checks {
  display: flex;
  gap: 20px;
  max-width: 980px;
  margin-top: 16px;
  padding: 16px;
  color: #475569;
}
</style>
`;
}

function assertNoForbiddenConsumerDependencies() {
  const packageJson = JSON.parse(readFileSync(join(appRoot, 'package.json'), 'utf8'));
  const deps = Object.keys(packageJson.dependencies || {});
  const forbidden = deps.filter(name =>
    name === '@mango/admin'
    || name === '@mango/admin-shell'
    || name === '@mango/admin-pages'
    || name.endsWith('-admin')
    || name === 'element-plus'
    || name === '@element-plus/icons-vue',
  );
  if (forbidden.length > 0) {
    fail(`API SDK consumer app has forbidden admin/UI dependencies: ${forbidden.join(', ')}`);
  }
}

function startConsumerDevServer() {
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
    env: process.env,
  });
  pipeToFile(child.stdout, join(evidenceRoot, 'frontend-dev.out'));
  pipeToFile(child.stderr, join(evidenceRoot, 'frontend-dev.err'));
  return child;
}

function runBrowserCheck() {
  const browserScript = join(evidenceRoot, 'api-sdk-consumer-browser-check.mjs');
  writeFileSync(browserScript, createBrowserScript(), 'utf8');
  run(process.execPath, [browserScript], {
    env: {
      ...process.env,
      MANGO_API_SDK_CONSUMER_URL: baseUrl,
      MANGO_API_SDK_CONSUMER_EVIDENCE: evidenceRoot,
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
const baseUrl = process.env.MANGO_API_SDK_CONSUMER_URL;
const evidenceRoot = process.env.MANGO_API_SDK_CONSUMER_EVIDENCE;
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1280, height: 820 } });
const consoleErrors = [];
const failedResponses = [];

page.on('console', (message) => {
  if (message.type() === 'error') consoleErrors.push(message.text());
});
page.on('pageerror', (error) => consoleErrors.push(error.message));
page.on('response', (response) => {
  if (response.status() >= 400) {
    failedResponses.push({ status: response.status(), url: response.url() });
  }
});

await page.goto(baseUrl, { waitUntil: 'networkidle' });
await page.locator('h1').filter({ hasText: 'Mango API SDK Consumer' }).waitFor({ state: 'visible', timeout: 15000 });
await page.locator('[data-testid="api-count"]').filter({ hasText: 'API packages: 9' }).waitFor({ state: 'visible', timeout: 15000 });
await page.locator('[data-testid="consumer-boundary"]').filter({ hasText: 'Admin shell imports: 0' }).waitFor({ state: 'visible', timeout: 15000 });
const runtime = await page.evaluate(() => window.__MANGO_API_SDK_CONSUMER__);
const layout = await page.evaluate(() => {
  const cards = Array.from(document.querySelectorAll('article')).map((element) => {
    const rect = element.getBoundingClientRect();
    const style = getComputedStyle(element);
    return {
      text: element.textContent,
      width: Math.round(rect.width),
      height: Math.round(rect.height),
      backgroundColor: style.backgroundColor,
      borderColor: style.borderColor,
    };
  });
  return {
    bodyText: document.body.innerText,
    cardCount: cards.length,
    cards,
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth + 2,
    backgroundColor: getComputedStyle(document.querySelector('.api-sdk-page')).backgroundColor,
  };
});
const report = { url: page.url(), runtime, layout, browser: { consoleErrors, failedResponses } };
if (runtime?.packageCount !== 9 || runtime?.sampleCount !== 10 || runtime?.hasAdminShell !== false) {
  throw new Error(\`Unexpected API SDK runtime state: \${JSON.stringify(runtime)}\`);
}
if (layout.cardCount !== 9 || layout.horizontalOverflow || !layout.bodyText.includes('@mango/system-api')) {
  throw new Error(\`Unexpected API SDK layout state: \${JSON.stringify(layout)}\`);
}
const unexpectedResponses = failedResponses.filter((item) => !item.url.endsWith('/favicon.ico'));
if (unexpectedResponses.length > 0) {
  throw new Error(\`Unexpected failed browser responses:\\n\${unexpectedResponses.map(item => \`\${item.status} \${item.url}\`).join('\\n')}\`);
}
const unexpectedErrors = consoleErrors.filter((message) => !message.includes('ResizeObserver loop'));
if (unexpectedErrors.length > 0) {
  throw new Error(\`Unexpected browser console errors:\\n\${unexpectedErrors.join('\\n')}\`);
}
await page.screenshot({ path: join(evidenceRoot, 'api-sdk-consumer-1280x820.png'), fullPage: true });
writeFileSync(join(evidenceRoot, 'layout-report.json'), \`\${JSON.stringify(report, null, 2)}\\n\`, 'utf8');
await browser.close();
`;
}

function writeSummary() {
  const summary = [
    '# Mango API SDK Consumer E2E',
    '',
    `- Base URL: ${baseUrl}`,
    '- Entry: temporary non-admin Vite application importing only `@mango/*-api` packages',
    '- Checks: package install, forbidden Admin dependency check, TypeScript type import, Vite production build, browser layout, screenshot',
    '- Evidence: install.out, typecheck.out, vite-build.out, api-sdk-consumer-1280x820.png, layout-report.json',
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

function ensurePlaywrightAvailable() {
  if (!existsSync(playwrightModulePath)) {
    fail(`Playwright module not found: ${playwrightModulePath}`);
  }
}

function resolvePlaywrightModulePath() {
  const directPath = join(repoRoot, 'node_modules/.pnpm/playwright@1.59.1/node_modules/playwright');
  if (existsSync(directPath)) return directPath;
  return dirname(require.resolve('playwright/package.json'));
}

function log(message) {
  console.log(`[api-sdk-consumer-e2e] ${message}`);
}

function fail(message) {
  throw new Error(message);
}
