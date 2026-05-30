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
import { dirname, join, relative, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const scriptFile = fileURLToPath(import.meta.url);
const repoRoot = resolve(dirname(scriptFile), '..');
const workspaceRoot = resolve(repoRoot, '..');
const backendMangoRoot = join(workspaceRoot, 'mango');
const packagesRoot = join(repoRoot, 'packages');
const createMangoAppCli = join(packagesRoot, 'create-mango-app/src/index.mjs');
const templateRoot = join(repoRoot, 'packages/create-mango-app/templates/mango-business-starter');
const args = parseArgs(process.argv.slice(2));
const tempRoot = args.keepTemp
  ? mkdirAndReturn(resolve(tmpdir(), `mango-dev-start-e2e-${Date.now()}`))
  : mkdtempSync(join(tmpdir(), 'mango-dev-start-e2e-'));
const evidenceRoot = args.evidenceDir ? resolve(args.evidenceDir) : join(tempRoot, 'evidence');
const projectName = 'dev-start-platform';
const projectRoot = join(tempRoot, projectName);
const dbName = args.dbName || `mango_dev_start_e2e_${Date.now().toString(36)}`;
const frontendUrl = `http://127.0.0.1:${args.frontendPort}`;
const backendUrl = `http://127.0.0.1:${args.backendPort}`;
const stagedPackageRoot = join(tempRoot, 'staged-packages');
const e2eAccount = {
  username: 'admin',
  password: 'admin123',
  tenantId: '1',
  tenantCode: 'default',
  realm: 'INTERNAL',
  actorType: 'INTERNAL_USER',
  partyType: 'INTERNAL_ORG',
  appCode: 'internal-admin',
};

let started = false;
let browserLayoutReport = null;
try {
  mkdirSync(evidenceRoot, { recursive: true });
  ensurePlaywrightAvailable();

  log(`Temporary root: ${tempRoot}`);
  log(`Evidence root: ${evidenceRoot}`);
  log(`Backend: ${backendUrl}`);
  log(`Frontend: ${frontendUrl}`);
  log(`Database: ${args.dbHost}:${args.dbPort}/${dbName}`);

  installBackendMangoArtifacts();
  run('pnpm', ['package:build'], { cwd: repoRoot });
  generateProject();
  rewriteWorkspaceDependencies();
  writeProjectEnv();

  run('node', ['scripts/check-template.mjs'], { cwd: projectRoot });
  run('pnpm', ['install', '--ignore-scripts'], {
    cwd: projectRoot,
    captureFile: join(evidenceRoot, 'install.out'),
  });
  run('pnpm', ['typecheck'], { cwd: projectRoot });
  run('pnpm', ['build'], { cwd: projectRoot });
  run('mvn', ['-f', 'backend/pom.xml', 'test'], {
    cwd: projectRoot,
    captureFile: join(evidenceRoot, 'maven-test.out'),
  });

  dropDatabaseIfExists();
  startProject();
  started = true;
  const authSession = loginBackendApi();
  verifyBackendApi(authSession);
  await runBrowserE2e();
  writeSummary();
  log('Business starter dev-start E2E passed.');
} finally {
  if (started) {
    copyProjectLog('backend.log');
    copyProjectLog('frontend.log');
  }
  if (started || existsSync(join(projectRoot, 'scripts/dev-stop.sh'))) {
    spawnSync('bash', ['scripts/dev-stop.sh'], { cwd: projectRoot, stdio: 'ignore' });
  }
  dropDatabaseIfExists();
  if (!args.keepTemp) {
    rmSync(tempRoot, { recursive: true, force: true });
  } else {
    log(`Kept temporary root: ${tempRoot}`);
  }
}

function parseArgs(argv) {
  const parsed = {
    backendPort: 5565,
    frontendPort: 5208,
    dbHost: '127.0.0.1',
    dbPort: 3306,
    dbUsername: 'root',
    dbPassword: '',
    dbName: '',
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
    if (arg === '--backend-port') {
      parsed.backendPort = Number(argv[index + 1]);
      index += 1;
      continue;
    }
    if (arg === '--frontend-port') {
      parsed.frontendPort = Number(argv[index + 1]);
      index += 1;
      continue;
    }
    if (arg === '--db-host') {
      parsed.dbHost = argv[index + 1] || '';
      index += 1;
      continue;
    }
    if (arg === '--db-port') {
      parsed.dbPort = Number(argv[index + 1]);
      index += 1;
      continue;
    }
    if (arg === '--db-username') {
      parsed.dbUsername = argv[index + 1] || '';
      index += 1;
      continue;
    }
    if (arg === '--db-password') {
      parsed.dbPassword = argv[index + 1] || '';
      index += 1;
      continue;
    }
    if (arg === '--db-name') {
      parsed.dbName = argv[index + 1] || '';
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
  if (!Number.isInteger(parsed.backendPort) || parsed.backendPort <= 0) {
    fail(`Invalid backend port: ${parsed.backendPort}`);
  }
  if (!Number.isInteger(parsed.frontendPort) || parsed.frontendPort <= 0) {
    fail(`Invalid frontend port: ${parsed.frontendPort}`);
  }
  if (parsed.dbName && !/^[A-Za-z0-9_]+$/.test(parsed.dbName)) {
    fail(`Invalid database name: ${parsed.dbName}`);
  }
  return parsed;
}

function generateProject() {
  run(process.execPath, [
    createMangoAppCli,
    'init',
    projectName,
    '--module',
    'guarantee',
    '--aggregate',
    'letter',
    '--package',
    'com.example.guarantee',
    '--group-id',
    'com.example',
    '--topology',
    'monolith',
    '--features',
    'base,system,rbac,workflow,notice,file',
    '--frontend-mode',
    'local',
    '--template',
    templateRoot,
  ], { cwd: tempRoot });
}

function installBackendMangoArtifacts() {
  if (!existsSync(join(backendMangoRoot, 'pom.xml'))) {
    fail(`Mango backend root not found: ${backendMangoRoot}`);
  }
  run('mvn', ['-q', '-pl', 'mango-admin-starter', '-am', '-DskipTests', 'install'], {
    cwd: backendMangoRoot,
    captureFile: join(evidenceRoot, 'mango-backend-install.out'),
  });
}

function rewriteWorkspaceDependencies() {
  const packageFiles = [
    join(projectRoot, 'frontend/apps/dev-start-platform-admin/package.json'),
    join(projectRoot, 'frontend/packages/guarantee/package.json'),
    join(projectRoot, 'frontend/packages/guarantee-api/package.json'),
  ];
  const stagedPackages = stageLocalMangoPackages();
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
    rewriteInternalPackageDependencies(stagedPackageJson, staged);
    writeFileSync(join(targetDir, 'package.json'), `${JSON.stringify(stagedPackageJson, null, 2)}\n`, 'utf8');
  }

  return staged;
}

function rewriteInternalPackageDependencies(packageJson, staged) {
  for (const section of ['dependencies', 'optionalDependencies', 'peerDependencies']) {
    if (!packageJson[section]) {
      continue;
    }
    for (const dependencyName of Object.keys(packageJson[section])) {
      if (staged.has(dependencyName)) {
        packageJson[section][dependencyName] = `file:${staged.get(dependencyName)}`;
      }
    }
  }
}

function writeProjectEnv() {
  writeFileSync(
    join(projectRoot, '.env'),
    [
      `MANGO_BACKEND_PORT=${args.backendPort}`,
      `MANGO_FRONTEND_PORT=${args.frontendPort}`,
      `MANGO_DB_HOST=${args.dbHost}`,
      `MANGO_DB_PORT=${args.dbPort}`,
      `MANGO_DB_NAME=${dbName}`,
      `MANGO_DB_USERNAME=${args.dbUsername}`,
      `MANGO_DB_PASSWORD=${args.dbPassword}`,
      'MANGO_DB_AUTO_CREATE=true',
      'MANGO_OFFICE_PLUGIN_ENABLED=false',
      'MANGO_START_TIMEOUT_SECONDS=240',
      `VITE_ADMIN_PROXY_PATH=${backendUrl}`,
      '',
    ].join('\n'),
    'utf8',
  );
}

function startProject() {
  const result = spawnSync('bash', ['scripts/dev-start.sh'], {
    cwd: projectRoot,
    encoding: 'utf8',
    stdio: 'pipe',
    timeout: 300_000,
  });
  writeFileSync(join(evidenceRoot, 'dev-start.out'), result.stdout || '', 'utf8');
  writeFileSync(join(evidenceRoot, 'dev-start.err'), result.stderr || '', 'utf8');
  copyProjectLog('backend.log');
  copyProjectLog('frontend.log');
  if (result.status !== 0) {
    process.stdout.write(result.stdout || '');
    process.stderr.write(result.stderr || '');
    fail('Generated project dev-start failed.');
  }
  waitForHttp(`${backendUrl}/actuator/health`);
  waitForHttp(frontendUrl);
}

function copyProjectLog(name) {
  const source = join(projectRoot, '.mango/logs', name);
  if (existsSync(source)) {
    writeFileSync(join(evidenceRoot, name), readFileSync(source, 'utf8'), 'utf8');
  }
}

function loginBackendApi() {
  const login = httpRequestJson(`${backendUrl}/auth/login`, {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify(e2eAccount),
  });
  assertApiSuccess(login, 'login');
  const accessToken = login.data?.accessToken || login.data?.token;
  if (!accessToken) {
    fail(`Login API did not return access token: ${JSON.stringify(maskAuthPayload(login))}`);
  }
  writeFileSync(
    join(evidenceRoot, 'auth-smoke.json'),
    `${JSON.stringify({
      success: Boolean(login.success || login.code === 200),
      userId: login.data?.userId,
      username: login.data?.username,
      tenantId: login.data?.tenantId,
      tenantCode: login.data?.tenantCode,
      roles: login.data?.roles || [],
      permissionsCount: Array.isArray(login.data?.permissions) ? login.data.permissions.length : 0,
      hasAccessToken: true,
    }, null, 2)}\n`,
    'utf8',
  );
  return {
    accessToken,
    tenantId: String(login.data?.tenantId || e2eAccount.tenantId),
  };
}

function verifyBackendApi(authSession) {
  const authHeaders = {
    Authorization: `Bearer ${authSession.accessToken}`,
    'X-Mango-Tenant-Id': authSession.tenantId,
    'TENANT-ID': authSession.tenantId,
  };
  const created = httpRequestJson(`${backendUrl}/guarantee/letters`, {
    method: 'POST',
    headers: { ...authHeaders, 'content-type': 'application/json' },
    body: JSON.stringify({ name: 'Sprint J Letter' }),
  });
  assertApiSuccess(created, 'create letter');
  const id = String(created.data?.id || '');
  if (!id || id === 'replace-with-generated-id') {
    fail(`Create API returned invalid id: ${JSON.stringify(created)}`);
  }
  if (created.data?.name !== 'Sprint J Letter') {
    fail(`Create API did not echo persisted name: ${JSON.stringify(created)}`);
  }

  const page = httpRequestJson(`${backendUrl}/guarantee/letters?pageNo=1&pageSize=20&name=Sprint%20J`, {
    method: 'GET',
    headers: authHeaders,
  });
  assertApiSuccess(page, 'page letters');
  const list = Array.isArray(page.data?.list) ? page.data.list : [];
  if (!list.some((item) => String(item.id) === id && item.name === 'Sprint J Letter')) {
    fail(`Page API did not return created record: ${JSON.stringify(page)}`);
  }

  const detail = httpRequestJson(`${backendUrl}/guarantee/letters/detail?id=${encodeURIComponent(id)}`, {
    method: 'GET',
    headers: authHeaders,
  });
  assertApiSuccess(detail, 'detail letter');
  if (String(detail.data?.id) !== id || detail.data?.name !== 'Sprint J Letter') {
    fail(`Detail API did not return created record: ${JSON.stringify(detail)}`);
  }

  const missing = httpRequest(`${backendUrl}/guarantee/letters/detail?id=missing-sprint-j`, {
    method: 'GET',
    headers: authHeaders,
  });
  if (missing.status < 400 && missing.body.includes('"success":true')) {
    fail(`Missing detail unexpectedly succeeded: ${missing.body}`);
  }

  writeFileSync(
    join(evidenceRoot, 'api-smoke.json'),
    `${JSON.stringify({ created, page, detail, missingStatus: missing.status }, null, 2)}\n`,
    'utf8',
  );
}

async function runBrowserE2e() {
  const require = createRequire(import.meta.url);
  const { chromium } = require(resolvePlaywrightModulePath());
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

  try {
    await page.goto(`${frontendUrl}/#/login`, { waitUntil: 'networkidle' });
    await page.locator('input[placeholder="用户名"]').fill(e2eAccount.username);
    await page.locator('input[placeholder="密码"]').fill(e2eAccount.password);
    const tenantOptionsResponse = page.waitForResponse((response) =>
      response.url().includes('/api/auth/login-institutions') && response.status() === 200
    );
    await page.locator('input[placeholder="密码"]').blur();
    await tenantOptionsResponse;
    await page.locator('.tenant-select').click();
    await page.getByRole('option', { name: /芒果集团/ }).click();
    await page.getByRole('button', { name: /登\s*录/ }).click();
    await page.waitForURL('**/#/home', { timeout: 30_000 });
    await page.waitForFunction(() => document.body.innerText.includes('首页'), undefined, { timeout: 30_000 });
    await page.goto(`${frontendUrl}/#/guarantee/letters`, { waitUntil: 'networkidle' });
    await assertPageContains(page, 'Letter名称');
    await assertPageContains(page, 'Sprint J Letter');
    browserLayoutReport = await assertMangoBusinessPageLayout(page);
    writeFileSync(
      join(evidenceRoot, 'layout-report.json'),
      `${JSON.stringify(browserLayoutReport, null, 2)}\n`,
      'utf8',
    );

    const unexpectedResponses = failedResponses.filter((item) => {
      const url = new URL(item.url);
      return url.pathname !== '/favicon.ico';
    });
    if (unexpectedResponses.length > 0) {
      throw new Error(`Unexpected browser failed responses:\n${unexpectedResponses.map(item => `${item.status} ${item.url}`).join('\n')}`);
    }
    if (consoleErrors.length > 0) {
      throw new Error(`Unexpected browser console errors:\n${consoleErrors.join('\n')}`);
    }

    await page.screenshot({ path: join(evidenceRoot, 'dev-start-frontend.png'), fullPage: true });
  } catch (error) {
    await writeBrowserFailureEvidence(page, error, failedResponses, consoleErrors);
    throw error;
  } finally {
    await browser.close();
  }
}

async function assertMangoBusinessPageLayout(page) {
  const report = await page.evaluate(() => {
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
        text: (element.textContent || '').trim().replace(/\s+/g, ' ').slice(0, 160),
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
    function styleSheetReport() {
      return Array.from(document.styleSheets).map((sheet) => {
        try {
          return { href: sheet.href, rules: sheet.cssRules?.length ?? null };
        } catch {
          return { href: sheet.href, inaccessible: true };
        }
      });
    }

    const shell = elementReport('.layout-container');
    const header = elementReport('.layout-header');
    const nav = elementReport('.layout-navbars-container');
    const aside = elementReport('.layout-aside');
    const main = elementReport('.layout-main');
    const search = elementReport('[data-mango-layout="search"]');
    const actions = elementReport('[data-mango-layout="actions"]');
    const table = elementReport('[data-mango-layout="table"]');
    const pagination = elementReport('[data-mango-layout="pagination"]');
    const listPage = elementReport('[data-mango-layout="list-page"]');
    const bodyText = document.body.innerText;
    const styleSheets = styleSheetReport();
    const brokenCssLinks = styleSheets.filter((item) => item.href && item.rules === 0);
    const verticalOrder =
      search.rect && actions.rect && table.rect && pagination.rect
        ? search.rect.top <= actions.rect.top
          && actions.rect.top <= table.rect.top
          && table.rect.top <= pagination.rect.top
        : false;
    const horizontalOverflow = document.documentElement.scrollWidth > window.innerWidth + 2;

    return {
      url: window.location.href,
      viewport: {
        width: window.innerWidth,
        height: window.innerHeight,
      },
      checks: {
        hasShellLayout: shell.visible,
        hasHeaderLayout: header.visible && header.rect.height >= 48 && header.rect.height <= 72,
        hasNavFlexLayout: nav.visible && nav.style.display === 'flex' && nav.style.backgroundColor === 'rgb(46, 92, 246)',
        hasAsideLayout: aside.visible && aside.rect.width >= 200 && aside.rect.width <= 240,
        hasMainLayout: main.visible && main.rect.x >= aside.rect.width,
        hasListPage: listPage.visible,
        hasElementPlusForm: Boolean(document.querySelector('[data-mango-layout="search"].el-form')),
        hasSearch: search.visible,
        hasActions: actions.visible,
        hasTable: table.visible && Boolean(document.querySelector('[data-mango-layout="table"].el-table')),
        hasPagination: pagination.visible && Boolean(document.querySelector('.el-pagination')),
        hasBusinessMenu: bodyText.includes('Letter管理'),
        hasBusinessRecord: bodyText.includes('Sprint J Letter'),
        hasQueryButton: bodyText.includes('查询'),
        hasResetButton: bodyText.includes('重置'),
        verticalOrder,
        hasNoBrokenCssLinks: brokenCssLinks.length === 0,
        hasAdminShellStyles: styleSheets.some((item) => !item.href && (item.rules || 0) > 200),
        noHorizontalOverflow: !horizontalOverflow,
      },
      elements: {
        shell,
        header,
        nav,
        aside,
        main,
        listPage,
        search,
        actions,
        table,
        pagination,
      },
      css: {
        brokenLinks: brokenCssLinks,
        styleSheets,
      },
    };
  });
  const failed = Object.entries(report.checks)
    .filter(([, passed]) => !passed)
    .map(([name]) => name);
  if (failed.length > 0) {
    throw new Error(`Mango business page layout checks failed: ${failed.join(', ')}. Report: ${JSON.stringify(report)}`);
  }
  return report;
}

async function assertPageContains(page, text) {
  try {
    await page.waitForFunction(
      expected => document.body.innerText.includes(expected),
      text,
      { timeout: 30_000 },
    );
  } catch (error) {
    const bodyText = await page.locator('body').innerText({ timeout: 5_000 }).catch(() => '');
    throw new Error(
      `Browser page did not contain "${text}". URL: ${page.url()}. Body excerpt: ${bodyText.slice(0, 1000)}`,
      { cause: error },
    );
  }
}

async function writeBrowserFailureEvidence(page, error, failedResponses, consoleErrors) {
  await page.screenshot({ path: join(evidenceRoot, 'dev-start-frontend-failure.png'), fullPage: true }).catch(() => {});
  const html = await page.content().catch(contentError => `Failed to read page content: ${contentError.message}`);
  const bodyText = await page.locator('body').innerText({ timeout: 5_000 }).catch(textError => `Failed to read body text: ${textError.message}`);
  writeFileSync(join(evidenceRoot, 'browser-failure.html'), html, 'utf8');
  writeFileSync(
    join(evidenceRoot, 'browser-failure.json'),
    `${JSON.stringify({
      message: error?.message,
      url: page.url(),
      bodyText: bodyText.slice(0, 4000),
      failedResponses,
      consoleErrors,
    }, null, 2)}\n`,
    'utf8',
  );
}

function assertApiSuccess(payload, label) {
  if (!payload?.success && payload?.code !== 200) {
    fail(`${label} API failed: ${JSON.stringify(maskAuthPayload(payload))}`);
  }
}

function maskAuthPayload(payload) {
  if (!payload || typeof payload !== 'object') {
    return payload;
  }
  const cloned = JSON.parse(JSON.stringify(payload));
  if (cloned.data && typeof cloned.data === 'object') {
    if (cloned.data.accessToken) {
      cloned.data.accessToken = '<masked>';
    }
    if (cloned.data.refreshToken) {
      cloned.data.refreshToken = '<masked>';
    }
    if (cloned.data.token) {
      cloned.data.token = '<masked>';
    }
  }
  return cloned;
}

function httpRequestJson(url, options) {
  const response = httpRequest(url, options);
  if (response.status < 200 || response.status >= 300) {
    fail(`HTTP ${response.status} for ${url}: ${response.body}`);
  }
  return JSON.parse(response.body);
}

function httpRequest(url, options) {
  const curlArgs = [
    '-sS',
    '--cookie-jar',
    '/dev/null',
    '-w',
    '\n%{http_code}',
    '-X',
    options.method || 'GET',
  ];
  for (const [name, value] of Object.entries(options.headers || {})) {
    curlArgs.push('-H', `${name}: ${value}`);
  }
  if (options.body) {
    curlArgs.push('--data', options.body);
  }
  curlArgs.push(url);
  const result = spawnSync('curl', curlArgs, { encoding: 'utf8', stdio: 'pipe' });
  if (result.status !== 0) {
    process.stderr.write(result.stderr || '');
    fail(`HTTP request failed: ${url}`);
  }
  const output = result.stdout || '';
  const newlineIndex = output.lastIndexOf('\n');
  return {
    body: output.slice(0, newlineIndex),
    status: Number(output.slice(newlineIndex + 1)),
  };
}

function dropDatabaseIfExists() {
  if (!dbName.startsWith('mango_dev_start_e2e_') && !args.dbName) {
    fail(`Refuse to drop unexpected database: ${dbName}`);
  }
  if (!commandExists('mysql')) {
    fail('mysql client not found; cannot manage E2E database.');
  }
  mysqlExec(`DROP DATABASE IF EXISTS \`${dbName}\`;`);
}

function mysqlExec(sql) {
  const mysqlArgs = [
    '--protocol=TCP',
    '-h',
    args.dbHost,
    '-P',
    String(args.dbPort),
    '-u',
    args.dbUsername,
    '-e',
    sql,
  ];
  const result = spawnSync('mysql', mysqlArgs, {
    encoding: 'utf8',
    stdio: 'pipe',
    env: {
      ...process.env,
      MYSQL_PWD: args.dbPassword,
    },
  });
  if (result.status !== 0) {
    process.stderr.write(result.stderr || '');
    fail(`mysql command failed: ${sql}`);
  }
}

function waitForHttp(url) {
  const deadline = Date.now() + 120_000;
  while (Date.now() < deadline) {
    const result = spawnSync('curl', ['-fsS', url], { stdio: 'ignore' });
    if (result.status === 0) {
      return;
    }
    sleep(500);
  }
  fail(`Timed out waiting for ${url}`);
}

function run(command, commandArgs, options = {}) {
  log(`$ ${command} ${commandArgs.join(' ')}`);
  const result = spawnSync(command, commandArgs, {
    cwd: options.cwd || repoRoot,
    env: options.env || process.env,
    stdio: options.captureFile ? 'pipe' : 'inherit',
    encoding: 'utf8',
  });
  if (options.captureFile) {
    writeFileSync(options.captureFile, `${result.stdout || ''}${result.stderr || ''}`, 'utf8');
  }
  if (result.status !== 0) {
    if (options.captureFile) {
      process.stdout.write(result.stdout || '');
      process.stderr.write(result.stderr || '');
    }
    fail(`Command failed: ${command} ${commandArgs.join(' ')}`);
  }
  return result;
}

function writeSummary() {
  const summary = [
    '# Mango Business Starter Dev Start E2E',
    '',
    `- Generated project: ${projectRoot}`,
    `- Backend: ${backendUrl}`,
    `- Frontend: ${frontendUrl}`,
    `- Database: ${args.dbHost}:${args.dbPort}/${dbName}`,
    '- Checks: Mango backend install, package build, project install, typecheck, build, Maven test, dev-start, health, real CRUD API, browser E2E',
    '- Browser page: /#/guarantee/letters',
    '- UI layout checks: Element Plus search form, action area, table, pagination, business menu, persisted business record, vertical order, horizontal overflow',
    `- Layout result: ${browserLayoutReport ? 'passed' : 'not recorded'}`,
    '- Evidence: mango-backend-install.out, dev-start.out, backend.log, frontend.log, maven-test.out, auth-smoke.json, api-smoke.json, layout-report.json, dev-start-frontend.png',
    '',
  ].join('\n');
  writeFileSync(join(evidenceRoot, 'summary.md'), summary, 'utf8');
}

function resolvePlaywrightModulePath() {
  const directPath = join(repoRoot, 'node_modules/.pnpm/playwright@1.59.1/node_modules/playwright');
  if (existsSync(join(directPath, 'package.json'))) {
    return directPath;
  }
  const require = createRequire(join(repoRoot, 'package.json'));
  return dirname(require.resolve('playwright/package.json'));
}

function ensurePlaywrightAvailable() {
  const modulePath = resolvePlaywrightModulePath();
  if (!existsSync(join(modulePath, 'package.json'))) {
    fail(`Playwright module not found: ${relative(repoRoot, modulePath)}`);
  }
}

function commandExists(command) {
  return spawnSync('command', ['-v', command], { shell: true, stdio: 'ignore' }).status === 0;
}

function mkdirAndReturn(path) {
  mkdirSync(path, { recursive: true });
  return path;
}

function sleep(ms) {
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);
}

function log(message) {
  console.log(`[dev-start-e2e] ${message}`);
}

function fail(message) {
  throw new Error(`[dev-start-e2e] ${message}`);
}
