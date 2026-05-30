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
import { dirname, join, relative, resolve } from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const scriptFile = fileURLToPath(import.meta.url);
const require = createRequire(import.meta.url);
const repoRoot = resolve(dirname(scriptFile), '..');
const workspaceRoot = resolve(repoRoot, '..');
const packagesRoot = join(repoRoot, 'packages');
const createMangoAppCli = join(packagesRoot, 'create-mango-app/src/index.mjs');
const templateRoot = join(repoRoot, 'packages/create-mango-app/templates/mango-business-starter');
const playwrightModulePath = resolvePlaywrightModulePath();
const builtInCapabilities = ['auth', 'rbac', 'system', 'workflow', 'notice', 'file'];
const args = parseArgs(process.argv.slice(2));
const tempRoot = args.keepTemp
  ? mkdirAndReturn(resolve(tmpdir(), `mango-generated-upgrade-e2e-${Date.now()}`))
  : mkdtempSync(join(tmpdir(), 'mango-generated-upgrade-e2e-'));
const evidenceRoot = resolve(args.evidenceDir || join(workspaceRoot, 'mango-docs/evidence/2026-05-30-sprint-7/generated-upgrade'));
const stagedPackageRoot = join(tempRoot, 'staged-packages');
const projectName = 'upgrade-platform';
const projectRoot = join(tempRoot, projectName);
const frontendRoot = join(projectRoot, 'frontend');
const appName = `${projectName}-admin`;
const appPackagePath = join(frontendRoot, 'apps', appName, 'package.json');
const appMainPath = join(frontendRoot, 'apps', appName, 'src/main.ts');
const frontendUrl = `http://127.0.0.1:${args.frontendPort}`;
let devServerProcess;
let browserReport;

try {
  mkdirSync(evidenceRoot, { recursive: true });
  ensurePlaywrightAvailable();
  log(`Temporary root: ${tempRoot}`);
  log(`Evidence root: ${evidenceRoot}`);
  log(`Frontend: ${frontendUrl}`);

  if (!args.skipPackageBuild) {
    run('pnpm', ['package:build'], { cwd: repoRoot });
  }
  run('pnpm', ['package:check'], { cwd: repoRoot });
  generateProject();
  const stagedPackages = stageLocalMangoPackages();
  rewriteWorkspaceDependencies(stagedPackages);
  run('node', ['scripts/check-template.mjs'], { cwd: projectRoot });

  applyLegacyCompatibilityProject(stagedPackages);
  run('pnpm', ['install', '--ignore-scripts'], {
    cwd: projectRoot,
    captureFile: join(evidenceRoot, 'legacy-install.out'),
  });
  run('pnpm', ['typecheck'], {
    cwd: projectRoot,
    captureFile: join(evidenceRoot, 'legacy-typecheck.out'),
  });
  run('pnpm', ['build'], {
    cwd: projectRoot,
    captureFile: join(evidenceRoot, 'legacy-build.out'),
  });

  applyRecommendedProject(stagedPackages);
  run('pnpm', ['install', '--ignore-scripts'], {
    cwd: projectRoot,
    captureFile: join(evidenceRoot, 'recommended-install.out'),
  });
  run('pnpm', ['typecheck'], {
    cwd: projectRoot,
    captureFile: join(evidenceRoot, 'recommended-typecheck.out'),
  });
  run('pnpm', ['build'], {
    cwd: projectRoot,
    captureFile: join(evidenceRoot, 'recommended-build.out'),
  });

  devServerProcess = startFrontendDevServer();
  waitForHttp(frontendUrl);
  runBrowserSmoke();
  writeSummary();
  log('Generated project upgrade E2E passed.');
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
    frontendPort: 5320,
    evidenceDir: '',
    skipPackageBuild: false,
    keepTemp: false,
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
    '--preset',
    'full',
    '--features',
    'base,system,rbac,workflow,notice,file',
    '--frontend-mode',
    'local',
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
    rewriteInternalPackageDependencies(stagedPackageJson, staged);
    writeFileSync(join(targetDir, 'package.json'), `${JSON.stringify(stagedPackageJson, null, 2)}\n`, 'utf8');
  }

  for (const requiredPackage of ['@mango/admin', '@mango/admin-pages', '@mango/app-runtime', ...builtInCapabilities.flatMap(name => [`@mango/${name}`, `@mango/${name}-api`, `@mango/${name}-admin`])]) {
    if (!staged.has(requiredPackage)) {
      fail(`Built Mango package not found: ${requiredPackage}`);
    }
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

function rewriteWorkspaceDependencies(stagedPackages) {
  for (const file of findPackageJsonFiles(frontendRoot)) {
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

function applyLegacyCompatibilityProject(stagedPackages) {
  const appPackage = JSON.parse(readFileSync(appPackagePath, 'utf8'));
  for (const capabilityName of builtInCapabilities) {
    const adminPackage = `@mango/${capabilityName}-admin`;
    const legacyPackage = `@mango/${capabilityName}`;
    delete appPackage.dependencies[adminPackage];
    appPackage.dependencies[legacyPackage] = `file:${stagedPackages.get(legacyPackage)}`;
  }
  writeFileSync(appPackagePath, `${JSON.stringify(appPackage, null, 2)}\n`, 'utf8');

  let mainSource = readFileSync(appMainPath, 'utf8');
  for (const capabilityName of builtInCapabilities) {
    const pascal = toPascalCase(capabilityName);
    mainSource = mainSource
      .replaceAll(`@mango/${capabilityName}-admin/capability`, `@mango/${capabilityName}/capability`)
      .replaceAll(`mango${pascal}AdminCapability`, `mango${pascal}Capability`);
  }
  writeFileSync(appMainPath, mainSource, 'utf8');
  assertLegacyCompatibilityProject();
  writeProjectSnapshot('legacy-compat-project.json');
}

function applyRecommendedProject(stagedPackages) {
  const appPackage = JSON.parse(readFileSync(appPackagePath, 'utf8'));
  for (const capabilityName of builtInCapabilities) {
    const legacyPackage = `@mango/${capabilityName}`;
    const adminPackage = `@mango/${capabilityName}-admin`;
    delete appPackage.dependencies[legacyPackage];
    appPackage.dependencies[adminPackage] = `file:${stagedPackages.get(adminPackage)}`;
  }
  writeFileSync(appPackagePath, `${JSON.stringify(appPackage, null, 2)}\n`, 'utf8');

  let mainSource = readFileSync(appMainPath, 'utf8');
  for (const capabilityName of builtInCapabilities) {
    const pascal = toPascalCase(capabilityName);
    mainSource = mainSource
      .replaceAll(`@mango/${capabilityName}/capability`, `@mango/${capabilityName}-admin/capability`)
      .replaceAll(`mango${pascal}Capability`, `mango${pascal}AdminCapability`);
  }
  writeFileSync(appMainPath, mainSource, 'utf8');
  assertRecommendedProject();
  writeProjectSnapshot('recommended-project.json');
}

function assertLegacyCompatibilityProject() {
  const appPackage = JSON.parse(readFileSync(appPackagePath, 'utf8'));
  const mainSource = readFileSync(appMainPath, 'utf8');
  for (const capabilityName of builtInCapabilities) {
    assert(Boolean(appPackage.dependencies[`@mango/${capabilityName}`]), `legacy project must depend on @mango/${capabilityName}`);
    assert(!appPackage.dependencies[`@mango/${capabilityName}-admin`], `legacy project must not depend on @mango/${capabilityName}-admin`);
    assert(mainSource.includes(`@mango/${capabilityName}/capability`), `legacy project must import @mango/${capabilityName}/capability`);
  }
  assert(mainSource.includes("from '@mango/admin'"), 'legacy compatibility project must still run through @mango/admin');
  assert(!mainSource.includes('@mango/admin-shell'), 'legacy compatibility project must not directly import @mango/admin-shell');
}

function assertRecommendedProject() {
  const appPackage = JSON.parse(readFileSync(appPackagePath, 'utf8'));
  const mainSource = readFileSync(appMainPath, 'utf8');
  for (const capabilityName of builtInCapabilities) {
    assert(Boolean(appPackage.dependencies[`@mango/${capabilityName}-admin`]), `recommended project must depend on @mango/${capabilityName}-admin`);
    assert(!appPackage.dependencies[`@mango/${capabilityName}`], `recommended project must not depend on legacy @mango/${capabilityName}`);
    assert(mainSource.includes(`@mango/${capabilityName}-admin/capability`), `recommended project must import @mango/${capabilityName}-admin/capability`);
  }
  assert(mainSource.includes("createMangoAdmin({"), 'recommended project must use createMangoAdmin');
  assert(mainSource.includes("from '@mango/admin'"), 'recommended project must import @mango/admin');
  assert(!mainSource.includes('@mango/admin-shell'), 'recommended project must not directly import @mango/admin-shell');
}

function writeProjectSnapshot(fileName) {
  const appPackage = JSON.parse(readFileSync(appPackagePath, 'utf8'));
  const mainSource = readFileSync(appMainPath, 'utf8');
  writeFileSync(
    join(evidenceRoot, fileName),
    `${JSON.stringify({
      appPackage: {
        name: appPackage.name,
        dependencies: appPackage.dependencies,
      },
      imports: [...mainSource.matchAll(/from ['"]([^'"]+)['"]/g)].map(match => match[1]),
      usesCreateMangoAdmin: mainSource.includes('createMangoAdmin({'),
      directAdminShellImport: mainSource.includes('@mango/admin-shell'),
    }, null, 2)}\n`,
    'utf8',
  );
}

function startFrontendDevServer() {
  const child = spawn('pnpm', [
    '--filter',
    appName,
    'dev',
    '--host',
    '127.0.0.1',
    '--port',
    String(args.frontendPort),
  ], {
    cwd: projectRoot,
    stdio: ['ignore', 'pipe', 'pipe'],
    env: {
      ...process.env,
      VITE_MANGO_E2E: 'true',
    },
  });
  pipeToFile(child.stdout, join(evidenceRoot, 'frontend-dev.out'));
  pipeToFile(child.stderr, join(evidenceRoot, 'frontend-dev.err'));
  return child;
}

function runBrowserSmoke() {
  const smokeFile = join(tempRoot, 'generated-upgrade-smoke.mjs');
  writeFileSync(smokeFile, createBrowserSmokeScript(), 'utf8');
  const reportFile = join(evidenceRoot, 'layout-report.json');
  run(process.execPath, [smokeFile], {
    cwd: projectRoot,
    env: {
      ...process.env,
      MANGO_UPGRADE_URL: frontendUrl,
      MANGO_UPGRADE_SCREENSHOT: join(evidenceRoot, 'recommended-frontend.png'),
      MANGO_UPGRADE_REPORT: reportFile,
      MANGO_PLAYWRIGHT_MODULE: playwrightModulePath,
    },
  });
  browserReport = JSON.parse(readFileSync(reportFile, 'utf8'));
}

function createBrowserSmokeScript() {
  return `
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.MANGO_PLAYWRIGHT_MODULE);
const baseUrl = process.env.MANGO_UPGRADE_URL;
const screenshot = process.env.MANGO_UPGRADE_SCREENSHOT;
const reportFile = process.env.MANGO_UPGRADE_REPORT;
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
await page.goto(\`\${baseUrl}/#/guarantee/letters\`, { waitUntil: 'networkidle' });
await page.waitForSelector('.shell-runtime-content', { timeout: 15000 });
await page.waitForFunction(() => document.body.innerText.includes('Letter管理'), undefined, { timeout: 15000 });
await page.waitForSelector('[data-mango-layout="list-page"]', { timeout: 15000 });

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
      text: (element.textContent || '').trim().replace(/\\s+/g, ' ').slice(0, 180),
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
  const bodyText = document.body.innerText;
  const shell = elementReport('.layout-container');
  const header = elementReport('.layout-header');
  const nav = elementReport('.layout-navbars-container');
  const aside = elementReport('.layout-aside');
  const main = elementReport('.layout-main');
  const runtime = elementReport('.shell-runtime-content');
  const listPage = elementReport('[data-mango-layout="list-page"]');
  const search = elementReport('[data-mango-layout="search"]');
  const actions = elementReport('[data-mango-layout="actions"]');
  const table = elementReport('[data-mango-layout="table"]');
  const pagination = elementReport('[data-mango-layout="pagination"]');
  const styleSheets = styleSheetReport();
  const brokenCssLinks = styleSheets.filter((item) => item.href && item.rules === 0);
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
      hasNavFlexLayout: nav.visible && nav.style.display === 'flex' && nav.rect.width >= 900,
      hasAsideLayout: aside.visible && aside.rect.width >= 200 && aside.rect.width <= 240,
      hasMainLayout: main.visible && main.rect.x >= aside.rect.width,
      hasRuntimeOutlet: runtime.visible,
      hasBusinessMenu: bodyText.includes('Letter管理'),
      hasBuiltInMenu: bodyText.includes('系统管理') || bodyText.includes('权限管理') || bodyText.includes('文件中心'),
      hasListPage: listPage.visible,
      hasSearch: search.visible,
      hasActions: actions.visible,
      hasTable: table.visible && Boolean(document.querySelector('[data-mango-layout="table"].el-table')),
      hasPagination: pagination.visible && Boolean(document.querySelector('.el-pagination')),
      hasExplicitApiErrorState: bodyText.includes('加载Letter列表失败') || bodyText.includes('Failed to fetch') || bodyText.includes('Internal Server Error') || bodyText.includes('500'),
      verticalOrder: Boolean(search.rect && actions.rect && table.rect && pagination.rect
        && search.rect.bottom <= actions.rect.top
        && actions.rect.bottom <= table.rect.top
        && table.rect.bottom <= pagination.rect.top),
      hasNoBrokenCssLinks: brokenCssLinks.length === 0,
      hasAdminShellStyles: styleSheets.some((item) => item.href && item.href.includes('@mango_admin-shell_style') && (item.rules || 0) > 20)
        || styleSheets.some((item) => item.href && item.href.includes('@mango/admin-shell') && (item.rules || 0) > 20)
        || styleSheets.some((item) => !item.href && (item.rules || 0) > 200),
      noHorizontalOverflow: !horizontalOverflow,
    },
    elements: {
      shell,
      header,
      nav,
      aside,
      main,
      runtime,
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
const failedChecks = Object.entries(report.checks)
  .filter(([, passed]) => !passed)
  .map(([name]) => name);
if (failedChecks.length > 0) {
  throw new Error(\`Generated upgrade layout checks failed: \${failedChecks.join(', ')}. Report: \${JSON.stringify(report)}\`);
}
const unexpectedErrors = consoleErrors.filter((message) => {
  if (message === 'Failed to load resource: the server responded with a status of 404 (Not Found)') {
    return false;
  }
  if (message.includes('Failed to load resource') && (message.includes('500') || message.includes('404'))) {
    return false;
  }
  if (message.includes('ERR_CONNECTION_REFUSED') || message.includes('Failed to fetch')) {
    return false;
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
  if (url.pathname.startsWith('/api/')) {
    return false;
  }
  return true;
});
if (unexpectedResponses.length > 0) {
  throw new Error(\`Unexpected failed browser responses:\\n\${unexpectedResponses.map(item => \`\${item.status} \${item.url}\`).join('\\n')}\`);
}
await page.screenshot({ path: screenshot, fullPage: true });
await import('node:fs').then(({ writeFileSync }) => {
  writeFileSync(reportFile, \`\${JSON.stringify({ ...report, browser: { consoleErrors, failedResponses } }, null, 2)}\\n\`, 'utf8');
});
await browser.close();
`;
}

function writeSummary() {
  const summary = [
    '# Mango Generated Project Upgrade E2E',
    '',
    `- Generated project: ${projectRoot}`,
    `- Frontend: ${frontendUrl}`,
    '- Checks: package check, generated project template check, legacy compatibility install/typecheck/build, recommended install/typecheck/build, browser smoke screenshot',
    '- Legacy compatibility path: @mango/*/capability and @mango/* packages still build through @mango/admin',
    '- Recommended path: @mango/*-admin/capability and @mango/*-api / @mango/*-admin packages',
    '- UI layout checks: Mango shell, header, nav, aside, runtime outlet, built-in menu, business menu, list page, search, actions, table, pagination, explicit API error state, no horizontal overflow',
    `- Layout result: ${browserReport ? 'passed' : 'not recorded'}`,
    '- Evidence: legacy-compat-project.json, legacy-install.out, legacy-typecheck.out, legacy-build.out, recommended-project.json, recommended-install.out, recommended-typecheck.out, recommended-build.out, frontend-dev.out, layout-report.json, recommended-frontend.png',
    '',
  ].join('\n');
  writeFileSync(join(evidenceRoot, 'summary.md'), summary, 'utf8');
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

function toPascalCase(value) {
  return value
    .split('-')
    .filter(Boolean)
    .map(part => part.charAt(0).toUpperCase() + part.slice(1))
    .join('');
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
    sleep(500);
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
  const deadline = Date.now() + 5_000;
  while (Date.now() < deadline) {
    const result = spawnSync('kill', ['-0', String(child.pid)], { stdio: 'ignore' });
    if (result.status !== 0) {
      return;
    }
    sleep(100);
  }
  child.kill('SIGKILL');
}

function resolvePlaywrightModulePath() {
  const directPath = join(repoRoot, 'node_modules/.pnpm/playwright@1.59.1/node_modules/playwright');
  if (existsSync(join(directPath, 'package.json'))) {
    return directPath;
  }
  return dirname(require.resolve('playwright/package.json'));
}

function ensurePlaywrightAvailable() {
  if (!existsSync(join(playwrightModulePath, 'package.json'))) {
    fail(`Playwright module not found: ${relative(workspaceRoot, playwrightModulePath)}`);
  }
}

function mkdirAndReturn(path) {
  mkdirSync(path, { recursive: true });
  return path;
}

function sleep(ms) {
  Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, ms);
}

function assert(condition, message) {
  if (!condition) {
    fail(message);
  }
}

function log(message) {
  console.log(`[generated-upgrade-e2e] ${message}`);
}

function fail(message) {
  throw new Error(`[generated-upgrade-e2e] ${message}`);
}
