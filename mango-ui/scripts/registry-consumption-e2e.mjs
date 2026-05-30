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
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const scriptFile = fileURLToPath(import.meta.url);
const repoRoot = resolve(dirname(scriptFile), '..');
const workspaceRoot = resolve(repoRoot, '..');
const packagesRoot = join(repoRoot, 'packages');
const createMangoAppCli = join(packagesRoot, 'create-mango-app/src/index.mjs');
const templateRoot = join(repoRoot, 'packages/create-mango-app/templates/mango-business-starter');
const playwrightModulePath = join(repoRoot, 'node_modules/.pnpm/playwright@1.59.1/node_modules/playwright');

const args = parseArgs(process.argv.slice(2));
const tempRoot = args.keepTemp
  ? mkdirAndReturn(resolve(tmpdir(), `mango-registry-e2e-${Date.now()}`))
  : mkdtempSync(join(tmpdir(), 'mango-registry-e2e-'));
const registryPort = args.registryPort;
const frontendPort = args.frontendPort;
const registryUrl = `http://127.0.0.1:${registryPort}`;
const registryTestVersion = `1.0.0-registry-e2e.${Date.now()}`;
const projectName = 'registry-platform';
const projectRoot = join(tempRoot, projectName);
const evidenceRoot = args.evidenceDir ? resolve(args.evidenceDir) : join(tempRoot, 'evidence');
const npmConfigPath = join(tempRoot, 'npmrc');
const pnpmStoreDir = join(tempRoot, 'pnpm-store');
const published = [];
const publishPackageNames = new Set();
const cleanupTasks = [];
let frontendSmokeReport = null;

let registryProcess;
let devServerProcess;
try {
  mkdirSync(evidenceRoot, { recursive: true });
  ensurePlaywrightAvailable();

  log(`Temporary root: ${tempRoot}`);
  log(`Evidence root: ${evidenceRoot}`);
  log(`Registry: ${registryUrl}`);

  run('pnpm', ['package:build'], { cwd: repoRoot });
  run('pnpm', ['package:check'], { cwd: repoRoot });

  registryProcess = startVerdaccio();
  cleanupTasks.push(() => stopProcess(registryProcess));
  waitForHttp(`${registryUrl}/-/ping`);
  const token = createRegistryUser();
  writeNpmConfig(token);

  const publishOrder = resolvePublishOrder();
  for (const { packageJson } of publishOrder) {
    publishPackageNames.add(packageJson.name);
  }
  for (const packageInfo of publishOrder) {
    publishPackage(packageInfo);
  }
  verifyRegistryViews();

  generateProject();
  rewriteProjectRegistryConfig();
  rewriteProjectMangoDependencyVersions();
  resetProjectInstallState();
  run('node', ['scripts/check-template.mjs'], { cwd: projectRoot });
  run('pnpm', ['install', '--ignore-scripts'], {
    cwd: projectRoot,
    env: {
      ...process.env,
      npm_config_userconfig: npmConfigPath,
      npm_config_store_dir: pnpmStoreDir,
    },
    captureFile: join(evidenceRoot, 'install.out'),
  });
  assertNoInstallPeerWarnings(join(evidenceRoot, 'install.out'));
  run('pnpm', ['typecheck'], { cwd: projectRoot });
  run('pnpm', ['build'], { cwd: projectRoot });

  runFrontendSmoke();

  writeSummary();
  log('Registry consumption E2E passed.');
} finally {
  stopProcess(devServerProcess);
  for (const cleanup of cleanupTasks.reverse()) {
    cleanup();
  }
  if (!args.keepTemp) {
    rmSync(tempRoot, { recursive: true, force: true });
  } else {
    log(`Kept temporary root: ${tempRoot}`);
  }
}

function parseArgs(argv) {
  const parsed = {
    registryPort: 4877,
    frontendPort: 5198,
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
    if (arg === '--registry-port') {
      parsed.registryPort = Number(argv[index + 1]);
      index += 1;
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
  if (!Number.isInteger(parsed.registryPort) || parsed.registryPort <= 0) {
    fail(`Invalid registry port: ${parsed.registryPort}`);
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

function startVerdaccio() {
  const configPath = join(tempRoot, 'verdaccio.yaml');
  writeFileSync(
    configPath,
    [
      'storage: ./storage',
      'auth:',
      '  htpasswd:',
      '    file: ./htpasswd',
      '    max_users: 1000',
      'uplinks:',
      '  npmjs:',
      '    url: https://registry.npmjs.org/',
      'packages:',
      "  '@mango/*':",
      '    access: $all',
      '    publish: $authenticated',
      '    unpublish: $authenticated',
      "  'create-mango-app':",
      '    access: $all',
      '    publish: $authenticated',
      '    unpublish: $authenticated',
      "  '**':",
      '    access: $all',
      '    publish: $authenticated',
      '    unpublish: $authenticated',
      '    proxy: npmjs',
      'log: { type: stdout, format: pretty, level: warn }',
      '',
    ].join('\n'),
    'utf8',
  );
  const child = spawn(
    'pnpm',
    ['dlx', 'verdaccio@6', '--config', configPath, '--listen', `127.0.0.1:${registryPort}`],
    {
      cwd: repoRoot,
      stdio: ['ignore', 'pipe', 'pipe'],
      env: process.env,
    },
  );
  pipeToFile(child.stdout, join(evidenceRoot, 'verdaccio.out'));
  pipeToFile(child.stderr, join(evidenceRoot, 'verdaccio.err'));
  child.on('exit', (code, signal) => {
    if (code !== null && code !== 0) {
      console.error(`Verdaccio exited with code ${code}`);
    }
    if (signal) {
      console.error(`Verdaccio exited with signal ${signal}`);
    }
  });
  return child;
}

function createRegistryUser() {
  const userName = 'mango-ci';
  const payload = JSON.stringify({
    name: userName,
    password: 'mango-ci-password',
    email: 'mango-ci@example.com',
    type: 'user',
    roles: [],
    date: new Date().toISOString(),
  });
  const response = httpRequestJson(`${registryUrl}/-/user/org.couchdb.user:${userName}`, {
    method: 'PUT',
    headers: { 'content-type': 'application/json' },
    body: payload,
  });
  if (!response.token) {
    fail(`Verdaccio user creation did not return token: ${JSON.stringify(response)}`);
  }
  return response.token;
}

function writeNpmConfig(token) {
  const host = `127.0.0.1:${registryPort}`;
  writeFileSync(
    npmConfigPath,
    [
      `registry=${registryUrl}/`,
      `@mango:registry=${registryUrl}/`,
      `//${host}/:_authToken=${token}`,
      `store-dir=${pnpmStoreDir}`,
      'strict-peer-dependencies=false',
      '',
    ].join('\n'),
    'utf8',
  );
}

function resolvePublishOrder() {
  const packageInfos = readdirSync(packagesRoot)
    .map((entry) => join(packagesRoot, entry))
    .filter((packageDir) => existsSync(join(packageDir, 'package.json')))
    .map((packageDir) => ({ packageDir, packageJson: readPackageJson(packageDir) }))
    .filter(({ packageJson }) => packageJson.name?.startsWith('@mango/'));
  const names = new Set(packageInfos.map(({ packageJson }) => packageJson.name));
  const dependencies = new Map(packageInfos.map(({ packageJson }) => [
    packageJson.name,
    Object.keys(packageJson.dependencies || {}).filter((name) => names.has(name)),
  ]));
  const consumers = new Map([...names].map((name) => [name, []]));
  for (const [name, deps] of dependencies) {
    for (const dep of deps) {
      consumers.get(dep).push(name);
    }
  }
  const indegrees = new Map([...names].map((name) => [name, dependencies.get(name).length]));
  const queue = [...names].filter((name) => indegrees.get(name) === 0).sort();
  const orderedNames = [];
  while (queue.length > 0) {
    const name = queue.shift();
    orderedNames.push(name);
    for (const consumer of consumers.get(name).sort()) {
      indegrees.set(consumer, indegrees.get(consumer) - 1);
      if (indegrees.get(consumer) === 0) {
        queue.push(consumer);
      }
    }
    queue.sort();
  }
  if (orderedNames.length !== packageInfos.length) {
    fail('Mango package dependency graph contains a cycle.');
  }
  return orderedNames.map((name) => packageInfos.find(({ packageJson }) => packageJson.name === name));
}

function publishPackage({ packageDir, packageJson }) {
  const stagingDir = join(tempRoot, 'publish', packageJson.name.replace('@', '').replace('/', '-'));
  rmSync(stagingDir, { recursive: true, force: true });
  mkdirSync(stagingDir, { recursive: true });
  cpSync(join(packageDir, 'dist'), join(stagingDir, 'dist'), { recursive: true });
  const publishPackageJson = {
    ...packageJson,
    version: registryTestVersion,
    publishConfig: { registry: registryUrl },
  };
  delete publishPackageJson.scripts;
  delete publishPackageJson.devDependencies;
  rewritePublishedInternalVersions(publishPackageJson);
  writeFileSync(join(stagingDir, 'package.json'), `${JSON.stringify(publishPackageJson, null, 2)}\n`, 'utf8');
  run('npm', [
    'publish',
    '--registry',
    registryUrl,
    '--access',
    'public',
    '--tag',
    'registry-e2e',
    '--ignore-scripts',
    '--userconfig',
    npmConfigPath,
  ], { cwd: stagingDir });
  published.push(`${packageJson.name}@${registryTestVersion}`);
}

function rewritePublishedInternalVersions(packageJson) {
  for (const section of ['dependencies', 'optionalDependencies', 'peerDependencies']) {
    const dependencies = packageJson[section];
    if (!dependencies) {
      continue;
    }
    for (const dependencyName of Object.keys(dependencies)) {
      if (publishPackageNames.has(dependencyName)) {
        dependencies[dependencyName] = registryTestVersion;
      }
    }
  }
}

function verifyRegistryViews() {
  for (const specifier of published) {
    const { packageName, version } = parsePublishedSpecifier(specifier);
    const metadata = httpRequestJson(`${registryUrl}/${encodeURIComponent(packageName)}`, {
      method: 'GET',
    });
    const publishedVersions = Object.keys(metadata.versions || {});
    if (!publishedVersions.includes(version)) {
      fail(`Registry view mismatch for ${packageName}: expected ${version}, got ${publishedVersions.join(', ')}`);
    }
  }
}

function parsePublishedSpecifier(specifier) {
  const separatorIndex = specifier.lastIndexOf('@');
  return {
    packageName: specifier.slice(0, separatorIndex),
    version: specifier.slice(separatorIndex + 1),
  };
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
    'mixed',
    '--template',
    templateRoot,
  ], { cwd: tempRoot });
}

function rewriteProjectRegistryConfig() {
  writeFileSync(
    join(projectRoot, '.npmrc'),
    [
      `registry=${registryUrl}/`,
      `@mango:registry=${registryUrl}/`,
      `store-dir=${pnpmStoreDir}`,
      'prefer-workspace-packages=true',
      'strict-peer-dependencies=false',
      '',
    ].join('\n'),
    'utf8',
  );
}

function rewriteProjectMangoDependencyVersions() {
  for (const file of findPackageJsonFiles(projectRoot)) {
    const packageJson = readJson(file);
    let changed = false;
    for (const section of ['dependencies', 'devDependencies', 'optionalDependencies', 'peerDependencies']) {
      const dependencies = packageJson[section];
      if (!dependencies) {
        continue;
      }
      for (const dependencyName of Object.keys(dependencies)) {
        if (publishPackageNames.has(dependencyName)) {
          dependencies[dependencyName] = registryTestVersion;
          changed = true;
        }
      }
    }
    if (changed) {
      writeJson(file, packageJson);
    }
  }
  writeProjectDependencySnapshot();
}

function resetProjectInstallState() {
  rmSync(join(projectRoot, 'node_modules'), { recursive: true, force: true });
  rmSync(join(projectRoot, 'pnpm-lock.yaml'), { force: true });
}

function writeProjectDependencySnapshot() {
  const packageSnapshots = findPackageJsonFiles(projectRoot)
    .map((file) => {
      const packageJson = readJson(file);
      return {
        path: relative(projectRoot, file),
        name: packageJson.name,
        dependencies: filterMangoDependencies(packageJson.dependencies),
        devDependencies: filterMangoDependencies(packageJson.devDependencies),
        optionalDependencies: filterMangoDependencies(packageJson.optionalDependencies),
        peerDependencies: filterMangoDependencies(packageJson.peerDependencies),
      };
    })
    .filter((item) => Object.values(item).some((value) => value && typeof value === 'object' && Object.keys(value).length > 0));
  writeFileSync(
    join(evidenceRoot, 'project-registry-dependencies.json'),
    `${JSON.stringify({
      registry: registryUrl,
      version: registryTestVersion,
      packages: packageSnapshots,
    }, null, 2)}\n`,
    'utf8',
  );
}

function filterMangoDependencies(dependencies) {
  if (!dependencies) {
    return {};
  }
  return Object.fromEntries(
    Object.entries(dependencies).filter(([name]) => name.startsWith('@mango/')),
  );
}

function findPackageJsonFiles(root) {
  const files = [];
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === '.git') {
      continue;
    }
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

function assertNoInstallPeerWarnings(filePath) {
  const text = readFileSync(filePath, 'utf8');
  if (/unmet peer/i.test(text)) {
    fail(`Registry install produced unmet peer warnings. See ${filePath}`);
  }
}

function runFrontendSmoke() {
  devServerProcess = spawn('pnpm', [
    '--filter',
    `${projectName}-admin`,
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
      npm_config_userconfig: npmConfigPath,
    },
  });
  pipeToFile(devServerProcess.stdout, join(evidenceRoot, 'frontend-dev.out'));
  pipeToFile(devServerProcess.stderr, join(evidenceRoot, 'frontend-dev.err'));
  waitForHttp(`http://127.0.0.1:${frontendPort}/`);

  const smokeFile = join(tempRoot, 'frontend-smoke.mjs');
  writeFileSync(smokeFile, createFrontendSmokeScript(), 'utf8');
  const reportFile = join(evidenceRoot, 'frontend-smoke-report.json');
  run(process.execPath, [smokeFile], {
    cwd: projectRoot,
    env: {
      ...process.env,
      MANGO_SMOKE_URL: `http://127.0.0.1:${frontendPort}/`,
      MANGO_SMOKE_SCREENSHOT: join(evidenceRoot, 'frontend-smoke.png'),
      MANGO_SMOKE_REPORT: reportFile,
      MANGO_PLAYWRIGHT_MODULE: playwrightModulePath,
    },
  });
  frontendSmokeReport = JSON.parse(readFileSync(reportFile, 'utf8'));
}

function createFrontendSmokeScript() {
  return `
import { createRequire } from 'node:module';

const require = createRequire(import.meta.url);
const { chromium } = require(process.env.MANGO_PLAYWRIGHT_MODULE);
const baseUrl = process.env.MANGO_SMOKE_URL;
const screenshot = process.env.MANGO_SMOKE_SCREENSHOT;
const reportFile = process.env.MANGO_SMOKE_REPORT;
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
async function readRuntimeDecision() {
  return page.evaluate(() => {
    const content = document.querySelector('.shell-runtime-content');
    return {
      pageType: content?.getAttribute('data-mango-runtime-page-type') || '',
      mode: content?.getAttribute('data-mango-runtime-mode') || '',
      moduleCode: content?.getAttribute('data-mango-runtime-module') || '',
      runtimeCode: content?.getAttribute('data-mango-runtime-code') || '',
      entry: content?.getAttribute('data-mango-runtime-entry') || '',
    };
  }).catch(() => ({
    pageType: '',
    mode: '',
    moduleCode: '',
    runtimeCode: '',
    entry: '',
  }));
}
async function writeFailureEvidence(error) {
  const { writeFileSync } = await import('node:fs');
  const failureScreenshot = screenshot.replace(/\\.png$/, '-failure.png');
  const failureReport = reportFile.replace(/\\.json$/, '-failure.json');
  const failureHtml = reportFile.replace(/\\.json$/, '-failure.html');
  const bodyText = await page.locator('body').innerText({ timeout: 1000 }).catch(() => '');
  const runtimeDecision = await readRuntimeDecision();
  await page.screenshot({ path: failureScreenshot, fullPage: true }).catch(() => undefined);
  writeFileSync(failureHtml, await page.content().catch(() => ''), 'utf8');
  writeFileSync(failureReport, \`\${JSON.stringify({
    url: page.url(),
    error: error instanceof Error ? error.message : String(error),
    runtimeDecision,
    bodyText: bodyText.slice(0, 3000),
    consoleErrors,
    failedResponses,
    screenshot: failureScreenshot,
    html: failureHtml,
  }, null, 2)}\\n\`, 'utf8');
}
try {
await page.goto(baseUrl, { waitUntil: 'networkidle' });
await page.waitForURL('**/#/home', { timeout: 15000 });
await page.waitForFunction(() => document.body.innerText.includes('首页'), undefined, { timeout: 15000 });
await page.goto(\`\${baseUrl}#/guarantee/letters\`, { waitUntil: 'networkidle' });
await page.waitForFunction(() => {
  const content = document.querySelector('.shell-runtime-content');
  return content?.getAttribute('data-mango-runtime-page-type') === 'MICRO_ROUTE';
}, undefined, { timeout: 15000 });
const runtimeDecision = await readRuntimeDecision();
if (runtimeDecision.moduleCode !== 'guarantee' || runtimeDecision.runtimeCode !== 'registry-platform-guarantee') {
  throw new Error(\`Unexpected runtime decision: \${JSON.stringify(runtimeDecision)}\`);
}
await page.waitForFunction(() => {
  const text = document.body.innerText;
  return text.includes('页面加载失败')
    && (text.includes('Failed to load Mango micro app') || text.includes('Mango micro app health check failed'));
}, undefined, { timeout: 30000 });
const bodyText = await page.locator('body').innerText({ timeout: 15000 });
if (!bodyText.includes('Letter管理')) {
  throw new Error(\`Expected generated business menu was not visible. Body: \${bodyText.slice(0, 1000)}\`);
}
const layoutReport = await page.evaluate((runtimeDecision) => {
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
  const runtime = elementReport('.shell-runtime-content');
  const menu = elementReport('.layout-aside, .layout-columns-aside');
  const runtimeEmpty = elementReport('.micro-runtime-empty');
  const runtimeRetry = elementReport('.micro-runtime-retry');
  const bodyText = document.body.innerText;
  const horizontalOverflow = document.documentElement.scrollWidth > window.innerWidth + 2;
  const styleSheets = styleSheetReport();
  const brokenCssLinks = styleSheets.filter((item) => item.href && item.rules === 0);
  return {
    url: window.location.href,
    runtimeDecision,
    viewport: {
      width: window.innerWidth,
      height: window.innerHeight,
    },
    checks: {
      hasShell: shell.visible,
      hasHeaderLayout: header.visible && header.rect.height >= 48 && header.rect.height <= 72,
      hasNavFlexLayout: nav.visible && nav.style.display === 'flex' && nav.style.backgroundColor === 'rgb(46, 92, 246)',
      hasAsideLayout: menu.visible && menu.rect.width >= 200 && menu.rect.width <= 240,
      hasRuntimeOutlet: runtime.visible,
      hasBusinessMenu: bodyText.includes('Letter管理'),
      hasExpectedMicroFailureState: bodyText.includes('页面加载失败')
        && (bodyText.includes('Failed to load Mango micro app') || bodyText.includes('Mango micro app health check failed')),
      hasStyledMicroErrorState: runtimeEmpty.visible
        && runtimeEmpty.style.display === 'flex'
        && runtimeEmpty.style.backgroundColor === 'rgb(255, 255, 255)'
        && runtimeEmpty.style.border.startsWith('1px')
        && runtimeRetry.visible
        && runtimeRetry.style.display.includes('flex')
        && runtimeRetry.style.backgroundColor === 'rgb(46, 92, 246)'
        && runtimeRetry.style.color === 'rgb(255, 255, 255)',
      hasNoBrokenCssLinks: brokenCssLinks.length === 0,
      hasAdminShellStyles: styleSheets.some((item) => !item.href && (item.rules || 0) > 200),
      noHorizontalOverflow: !horizontalOverflow,
    },
    elements: {
      shell,
      header,
      nav,
      runtime,
      menu,
      runtimeEmpty,
      runtimeRetry,
    },
    css: {
      brokenLinks: brokenCssLinks,
      styleSheets,
    },
  };
}, runtimeDecision);
const failedLayoutChecks = Object.entries(layoutReport.checks)
  .filter(([, passed]) => !passed)
  .map(([name]) => name);
if (failedLayoutChecks.length > 0) {
  throw new Error(\`Registry frontend smoke layout checks failed: \${failedLayoutChecks.join(', ')}. Report: \${JSON.stringify(layoutReport)}\`);
}
function isAllowedFailedResponse(item) {
  const url = new URL(item.url);
  if (url.origin === 'http://127.0.0.1:5190') {
    return true;
  }
  if (url.pathname === '/api/authorization/menus/user') {
    return true;
  }
  if (url.pathname === '/favicon.ico') {
    return true;
  }
  return false;
}
const hasOnlyAllowedFailedResponses = failedResponses.every(isAllowedFailedResponse);
const unexpectedErrors = consoleErrors.filter((message) => {
  if (message === 'Failed to load resource: the server responded with a status of 404 (Not Found)') {
    return false;
  }
  if (
    hasOnlyAllowedFailedResponses
    && message === 'Failed to load resource: the server responded with a status of 500 (Internal Server Error)'
  ) {
    return false;
  }
  return !message.includes('Failed to load Mango micro app')
    && !message.includes('Failed to mount Mango micro app')
    && !message.includes('Mango micro app health check failed')
    && !message.includes('ERR_CONNECTION_REFUSED');
});
if (unexpectedErrors.length > 0) {
  throw new Error(\`Unexpected browser console errors:\\n\${unexpectedErrors.join('\\n')}\`);
}
const unexpectedResponses = failedResponses.filter((item) => !isAllowedFailedResponse(item));
if (unexpectedResponses.length > 0) {
  throw new Error(\`Unexpected failed browser responses:\\n\${unexpectedResponses.map(item => \`\${item.status} \${item.url}\`).join('\\n')}\`);
}
await page.screenshot({ path: screenshot, fullPage: true });
await import('node:fs').then(({ writeFileSync }) => {
  writeFileSync(reportFile, \`\${JSON.stringify(layoutReport, null, 2)}\\n\`, 'utf8');
});
} catch (error) {
  await writeFailureEvidence(error);
  throw error;
}
await browser.close();
`;
}

function writeSummary() {
  const summary = [
    '# Mango Registry Consumption E2E',
    '',
    `- Registry: ${registryUrl}`,
    `- Test version: ${registryTestVersion}`,
    `- Generated project: ${projectRoot}`,
    `- Published packages: ${published.length}`,
    ...published.map((item) => `  - ${item}`),
    '- Checks: package build, package contract, registry publish/view, generated project install, typecheck, build, browser smoke',
    `- Browser page: ${frontendSmokeReport?.url || `http://127.0.0.1:${frontendPort}/#/guarantee/letters`}`,
    '- UI layout checks: admin shell, runtime outlet, business menu, expected micro app failure state, horizontal overflow',
    `- Layout result: ${frontendSmokeReport ? 'passed' : 'not recorded'}`,
    '- Evidence: project-registry-dependencies.json, install.out, verdaccio.out, frontend-dev.out, frontend-smoke-report.json, frontend-smoke.png',
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

function httpRequestJson(url, options) {
  const curlArgs = [
    '-fsS',
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
  return JSON.parse(result.stdout);
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
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 100);
  }
  child.kill('SIGKILL');
}

function ensurePlaywrightAvailable() {
  if (!existsSync(playwrightModulePath)) {
    fail(`Playwright module not found: ${relative(workspaceRoot, playwrightModulePath)}`);
  }
}

function readPackageJson(packageDir) {
  return JSON.parse(readFileSync(join(packageDir, 'package.json'), 'utf8'));
}

function readJson(file) {
  return JSON.parse(readFileSync(file, 'utf8'));
}

function writeJson(file, value) {
  writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
}

function log(message) {
  console.log(`[registry-e2e] ${message}`);
}

function fail(message) {
  throw new Error(`[registry-e2e] ${message}`);
}
