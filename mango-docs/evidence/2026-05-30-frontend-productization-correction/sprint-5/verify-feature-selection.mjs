import { mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { execFileSync, spawn } from 'node:child_process';
import { tmpdir } from 'node:os';
import { createRequire } from 'node:module';

const repoRoot = process.cwd();
const require = createRequire(join(repoRoot, 'mango-ui/node_modules/.pnpm/node_modules/playwright/package.json'));
const { chromium, request: playwrightRequest } = require('playwright');

const evidenceDir = join(repoRoot, 'mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5');
const reportPath = join(evidenceDir, 'feature-selection-report.json');
const screenshotDir = join(evidenceDir, 'screenshots');
const workspaceRoot = join(tmpdir(), `mango-sprint-5-feature-selection-${Date.now()}`);
const appDir = join(workspaceRoot, 'app');
const frontendPort = Number(process.env.MANGO_S5_FEATURE_PORT || 18615);
const frontendUrl = `http://127.0.0.1:${frontendPort}`;
const backendUrl = process.env.MANGO_BACKEND_URL || 'http://127.0.0.1:18800';
const mangoPackageOverrides = {
  '@mango/admin': `file:${join(repoRoot, 'mango-ui/packages/admin')}`,
  '@mango/admin-shell': `file:${join(repoRoot, 'mango-ui/packages/admin-shell')}`,
  '@mango/admin-pages': `file:${join(repoRoot, 'mango-ui/packages/admin-pages')}`,
  '@mango/api-schema': `file:${join(repoRoot, 'mango-ui/packages/api-schema')}`,
  '@mango/app-runtime': `file:${join(repoRoot, 'mango-ui/packages/app-runtime')}`,
  '@mango/auth': `file:${join(repoRoot, 'mango-ui/packages/auth')}`,
  '@mango/calendar': `file:${join(repoRoot, 'mango-ui/packages/calendar')}`,
  '@mango/common': `file:${join(repoRoot, 'mango-ui/packages/common')}`,
  '@mango/file': `file:${join(repoRoot, 'mango-ui/packages/file')}`,
  '@mango/notice': `file:${join(repoRoot, 'mango-ui/packages/notice')}`,
  '@mango/numgen': `file:${join(repoRoot, 'mango-ui/packages/numgen')}`,
  '@mango/rbac': `file:${join(repoRoot, 'mango-ui/packages/rbac')}`,
  '@mango/system': `file:${join(repoRoot, 'mango-ui/packages/system')}`,
  '@mango/template': `file:${join(repoRoot, 'mango-ui/packages/template')}`,
  '@mango/workflow': `file:${join(repoRoot, 'mango-ui/packages/workflow')}`,
  '@mango/workflow-business-example': `file:${join(repoRoot, 'mango-ui/packages/workflow-business-example')}`,
};
const selectedFeaturePackages = {
  '@mango/workflow': mangoPackageOverrides['@mango/workflow'],
  '@mango/workflow-business-example': mangoPackageOverrides['@mango/workflow-business-example'],
};
const workflowExpenseApplyQuery = 'definitionId=2060687058739757058&definitionKey=expense_reimbursement&applyPageKey=workflow.expense.apply';

const commands = [];
const checks = [];
const errors = [];
let devServer;

function run(command, args, options = {}) {
  const startedAt = new Date().toISOString();
  try {
    const stdout = execFileSync(command, args, {
      cwd: options.cwd || repoRoot,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      env: { ...process.env, ...options.env },
    });
    commands.push({ command, args, cwd: options.cwd || repoRoot, ok: true, startedAt, stdout });
    return stdout;
  } catch (error) {
    commands.push({
      command,
      args,
      cwd: options.cwd || repoRoot,
      ok: false,
      startedAt,
      stdout: error.stdout?.toString() || '',
      stderr: error.stderr?.toString() || '',
      status: error.status,
    });
    throw error;
  }
}

function writeJson(filePath, value) {
  writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`);
}

async function waitForHttp(url, timeoutMs = 45000) {
  const started = Date.now();
  let lastError;
  while (Date.now() - started < timeoutMs) {
    try {
      const response = await fetch(url, { cache: 'no-store' });
      if (response.ok) {
        return;
      }
      lastError = new Error(`${url} returned ${response.status}`);
    } catch (error) {
      lastError = error;
    }
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  throw lastError || new Error(`Timed out waiting for ${url}`);
}

async function login(page) {
  await page.goto(`${frontendUrl}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.waitForSelector('.login-container', { timeout: 20000 });
  await page.locator('input[placeholder="用户名"]').fill(process.env.MANGO_E2E_USERNAME || 'admin');
  await page.locator('input[placeholder="密码"]').fill(process.env.MANGO_E2E_PASSWORD || 'admin123');
  await page.locator('input[placeholder="密码"]').blur();
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: new RegExp(process.env.MANGO_E2E_TENANT_NAME || '芒果集团') }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 20000 });
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined);
}

async function waitForScreenshotReady(page) {
  await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined);
  await page.locator('.el-message').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => undefined);
  await page.locator('.el-loading-mask').waitFor({ state: 'hidden', timeout: 5000 }).catch(() => undefined);
}

async function getBackendMenuTopNames() {
  const api = await playwrightRequest.newContext({ baseURL: backendUrl });
  try {
    const loginResponse = await api.post('/auth/login', {
      data: {
        username: process.env.MANGO_E2E_USERNAME || 'admin',
        password: process.env.MANGO_E2E_PASSWORD || 'admin123',
        tenantCode: process.env.MANGO_E2E_TENANT_CODE || 'default',
        realm: 'INTERNAL',
        actorType: 'INTERNAL_USER',
        partyType: 'INTERNAL_ORG',
        appCode: 'internal-admin',
      },
    });
    const loginBody = await loginResponse.json();
    const token = loginBody?.data?.accessToken || loginBody?.data?.token;
    const menuResponse = await api.get('/authorization/menus/user', {
      params: { fmt: 'tree', appCode: 'internal-admin' },
      headers: { Authorization: `Bearer ${token}` },
    });
    const menuBody = await menuResponse.json();
    return (menuBody?.data || []).map(menu => menu.menuName).filter(Boolean);
  } finally {
    await api.dispose();
  }
}

function prepareApp(scenario) {
  rmSync(workspaceRoot, { recursive: true, force: true });
  mkdirSync(join(appDir, 'src'), { recursive: true });
  mkdirSync(join(appDir, 'public'), { recursive: true });
  writeFileSync(join(appDir, 'index.html'), '<!doctype html><html><body><div id="app"></div><script type="module" src="/src/main.ts"></script></body></html>\n');
  writeJson(join(appDir, 'package.json'), {
    name: 'mango-sprint-5-feature-selection',
    version: '1.0.0',
    private: true,
    type: 'module',
    scripts: {
      dev: `vite --host 127.0.0.1 --port ${frontendPort} --strictPort`,
      build: 'vite build',
    },
    dependencies: {
      '@mango/admin': mangoPackageOverrides['@mango/admin'],
      '@vitejs/plugin-vue': '^5.2.1',
      typescript: '^5.6.3',
      vite: '^6.0.1',
      vue: '3.5.13',
      'vue-router': '^4.1.6',
      pinia: '2.0.32',
      'vue-i18n': '9.2.2',
      'element-plus': '2.5.5',
      ...(scenario.dependencies || {}),
    },
    devDependencies: {},
    pnpm: {
      overrides: mangoPackageOverrides,
    },
  });
  writeFileSync(join(appDir, 'src/main.ts'), `import { createMangoAdminApp } from '@mango/admin';
${scenario.imports.join('\n')}

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: '/api',
  title: 'Mango Feature Selection',
  ${scenario.featuresExpression ? `features: ${scenario.featuresExpression},` : ''}
  ${scenario.featureRegistrarsExpression ? `featureRegistrars: ${scenario.featureRegistrarsExpression},` : ''}
  devCenter: {
    deployEnv: import.meta.env.VITE_MANGO_DEPLOY_ENV || import.meta.env.MODE,
  },
}).mount();
`);
  writeFileSync(join(appDir, 'vite.config.ts'), `import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: '${backendUrl}',
        changeOrigin: true,
        rewrite: path => path.replace(/^\\/api/, ''),
      },
    },
  },
});
`);
}

async function runScenario(scenario) {
  prepareApp(scenario);
  run('pnpm', ['install', '--ignore-workspace'], { cwd: appDir });
  run('pnpm', ['run', 'build'], { cwd: appDir });
  devServer = spawn('pnpm', ['run', 'dev'], {
    cwd: appDir,
    env: {
      ...process.env,
      VITE_MANGO_DEPLOY_ENV: 'dev',
      VITE_MANGO_RBAC_MODE: 'local',
      VITE_MANGO_SYSTEM_MODE: 'local',
      VITE_MANGO_WORKFLOW_MODE: 'local',
    },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  await waitForHttp(frontendUrl);
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } });
  try {
    await login(page);
    if (scenario.afterLogin) {
      await scenario.afterLogin(page);
    }
    await waitForScreenshotReady(page);
    await page.screenshot({ path: join(screenshotDir, `s5-feature-${scenario.name}.png`), fullPage: true });
    const topMenus = await page.locator('.layout-top-system-item').evaluateAll(nodes => nodes.map(node => node.textContent?.trim()).filter(Boolean));
    const pass =
      scenario.expectedTopMenus.every(menu => topMenus.includes(menu))
      && scenario.rejectedTopMenus.every(menu => !topMenus.includes(menu));
    checks.push({
      name: scenario.name,
      url: page.url(),
      topMenus,
      expectedTopMenus: scenario.expectedTopMenus,
      rejectedTopMenus: scenario.rejectedTopMenus,
      pass,
    });
  } finally {
    await browser.close();
    await stopDevServer();
  }
}

async function stopDevServer() {
  const server = devServer;
  devServer = undefined;
  if (!server || server.killed) {
    return;
  }
  await new Promise((resolve) => {
    const timer = setTimeout(() => {
      if (!server.killed) {
        server.kill('SIGKILL');
      }
      resolve(undefined);
    }, 5000);
    server.once('exit', () => {
      clearTimeout(timer);
      resolve(undefined);
    });
    server.kill('SIGTERM');
  });
}

async function main() {
  mkdirSync(screenshotDir, { recursive: true });
  const backendTopMenus = await getBackendMenuTopNames();
  await runScenario({
    name: 'core',
    imports: ["import '@mango/admin/style.css';"],
    expectedTopMenus: ['首页', '系统管理', '开发中心'],
    rejectedTopMenus: ['审批中心', '平台能力', '通知中心'],
  });
  await runScenario({
    name: 'workflow-only',
    dependencies: selectedFeaturePackages,
    imports: [
      "import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';",
      "import { registerMangoWorkflowBusinessExampleAdminPages } from '@mango/workflow-business-example/admin-pages';",
      "import '@mango/admin/style.css';",
      "import '@mango/workflow/style.css';",
      "import '@mango/workflow-business-example/style.css';",
    ],
    featuresExpression: "['workflow']",
    featureRegistrarsExpression: '[registerMangoWorkflowAdminPages, registerMangoWorkflowBusinessExampleAdminPages]',
    expectedTopMenus: ['首页', '系统管理', '审批中心', '开发中心'],
    rejectedTopMenus: ['平台能力', '通知中心'],
    async afterLogin(page) {
      const approvalCenter = page.locator('.layout-top-system-item').filter({ hasText: '审批中心' });
      await approvalCenter.click();
      await page.goto(`${frontendUrl}/#/workflow/custom-apply?${workflowExpenseApplyQuery}`, { waitUntil: 'domcontentloaded' });
      await page.waitForLoadState('networkidle', { timeout: 15000 }).catch(() => undefined);
      await page.getByText('费用报销申请').waitFor({ timeout: 20000 });
      await page.getByRole('button', { name: '申请报销' }).waitFor({ timeout: 20000 });
      const unregisteredCount = await page.getByText('自定义申请页未注册').count();
      if (unregisteredCount > 0) {
        throw new Error('Workflow business example apply page was not registered');
      }
    },
  });
  await runScenario({
    name: 'full',
    dependencies: {
      '@mango/calendar': mangoPackageOverrides['@mango/calendar'],
      '@mango/file': mangoPackageOverrides['@mango/file'],
      '@mango/notice': mangoPackageOverrides['@mango/notice'],
      '@mango/numgen': mangoPackageOverrides['@mango/numgen'],
      '@mango/template': mangoPackageOverrides['@mango/template'],
      '@mango/workflow': mangoPackageOverrides['@mango/workflow'],
      '@mango/workflow-business-example': mangoPackageOverrides['@mango/workflow-business-example'],
    },
    imports: [
      "import { mangoFullAdminFeatureRegistrars } from '@mango/admin/full';",
      "import '@mango/admin/style-full.css';",
    ],
    featuresExpression: "'full'",
    featureRegistrarsExpression: 'mangoFullAdminFeatureRegistrars',
    expectedTopMenus: ['首页', ...backendTopMenus, '开发中心'],
    rejectedTopMenus: [],
  });
  const report = {
    ok: checks.every(check => check.pass),
    checkedAt: new Date().toISOString(),
    backendUrl,
    frontendUrl,
    workspaceRoot,
    checks,
    commands,
    errors,
  };
  writeJson(reportPath, report);
  if (!report.ok) {
    throw new Error(`Feature selection failed: ${reportPath}`);
  }
  console.log(`Feature selection report: ${reportPath}`);
}

main().catch((error) => {
  errors.push(error.stack || error.message);
  writeJson(reportPath, {
    ok: false,
    checkedAt: new Date().toISOString(),
    backendUrl,
    frontendUrl,
    workspaceRoot,
    checks,
    commands,
    errors,
  });
  if (devServer && !devServer.killed) {
    devServer.kill('SIGTERM');
  }
  console.error(error);
  process.exitCode = 1;
});
