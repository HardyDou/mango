#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import dns from 'node:dns/promises';
import { existsSync, mkdirSync, rmSync } from 'node:fs';
import https from 'node:https';
import { networkInterfaces } from 'node:os';
import { join } from 'node:path';
import {
  assertGeneratedProjectBoundary,
  assertInstalledSymlinkBoundary,
  readJson,
  sha256File,
  writeJson,
} from './business-lab-lib.mjs';

const runtimeRoot = '/runtime';
const projectName = 'frontend-standards-business-lab';
const projectRoot = join(runtimeRoot, 'projects', projectName);
const frontendRoot = join(projectRoot, 'frontend');
const dependencyStore = join(runtimeRoot, 'package-store', projectName);
const reportRoot = join(runtimeRoot, 'frontend-quality', 'business-lab');
const reportPath = join(reportRoot, 'sealed-report.json');
const preparationReportPath = join(reportRoot, 'preparation-report.json');
const commandResults = [];

function run(command, args, options = {}) {
  const startedAt = Date.now();
  const result = spawnSync(command, args, {
    cwd: options.cwd || projectRoot,
    encoding: 'utf8',
    env: { ...process.env, FORCE_COLOR: '0', ...options.env },
    maxBuffer: 64 * 1024 * 1024,
  });
  const entry = {
    command: [command, ...args].join(' '),
    status: result.status ?? 1,
    durationMs: Date.now() - startedAt,
  };
  commandResults.push(entry);
  if (result.stdout) process.stdout.write(result.stdout);
  if (result.stderr) process.stderr.write(result.stderr);
  if (entry.status !== 0 && !options.allowFailure) {
    throw new Error(`${entry.command} failed with status ${entry.status}`);
  }
  return { ...result, ...entry };
}

async function dnsCanary() {
  try {
    await dns.lookup('registry.npmjs.org');
    return { attempted: true, blocked: false, errorCode: '' };
  } catch (error) {
    return { attempted: true, blocked: true, errorCode: error.code || error.name };
  }
}

function httpsCanary() {
  return new Promise((resolve) => {
    const request = https.get(
      {
        hostname: '1.1.1.1',
        path: '/',
        port: 443,
        rejectUnauthorized: false,
        timeout: 3000,
      },
      (response) => {
        response.resume();
        resolve({ attempted: true, blocked: false, statusCode: response.statusCode || 0, errorCode: '' });
      },
    );
    request.on('timeout', () => request.destroy(new Error('HTTPS_CANARY_TIMEOUT')));
    request.on('error', (error) => {
      resolve({ attempted: true, blocked: true, statusCode: 0, errorCode: error.code || error.message });
    });
  });
}

function assertLoopbackOnly() {
  const interfaces = networkInterfaces();
  const external = Object.entries(interfaces)
    .flatMap(([name, addresses]) => (addresses || []).map((address) => ({ name, ...address })))
    .filter((address) => !address.internal);
  if (external.length > 0) {
    throw new Error(`sealed network namespace exposes non-loopback interfaces: ${JSON.stringify(external)}`);
  }
  return Object.keys(interfaces).sort();
}

async function waitForShell(url, timeoutMs = 60000) {
  const deadline = Date.now() + timeoutMs;
  let lastError = '';
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url);
      const body = await response.text();
      if (response.ok && /<div\s+id=["']app["']/iu.test(body)) {
        return { url, status: response.status, bodyBytes: Buffer.byteLength(body) };
      }
      lastError = `status=${response.status}`;
    } catch (error) {
      lastError = error.message;
    }
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  throw new Error(`minimal shell did not become ready at ${url}: ${lastError}`);
}

function writeLocalFrontendOverride(appName) {
  const mangoDir = join(projectRoot, '.mango');
  mkdirSync(mangoDir, { recursive: true });
  writeJson(join(mangoDir, 'dev-workspace.local.json'), {
    apps: {
      [appName]: {
        dependsOn: [],
      },
    },
  });
}

function safeWorkspaceEvidence(workspace) {
  return {
    workspaceId: workspace.workspaceId,
    slot: workspace.slot,
    backendPort: workspace.backendPort,
    frontendPort: workspace.frontendPort,
    dbName: workspace.dbName,
  };
}

mkdirSync(reportRoot, { recursive: true });
const report = {
  schemaVersion: 1,
  status: 'failed',
  generatedAt: new Date().toISOString(),
  networkMode: 'deny-all',
  commands: commandResults,
};

let startedApp = '';
try {
  if (!existsSync(preparationReportPath)) {
    throw new Error('Business Lab preparation report is missing');
  }
  const preparation = readJson(preparationReportPath);
  if (preparation.platform !== process.platform || preparation.arch !== process.arch) {
    throw new Error(
      `preparation platform ${preparation.platform}/${preparation.arch} does not match sealed ` +
        `${process.platform}/${process.arch}`,
    );
  }
  report.preparationReportSha256 = sha256File(preparationReportPath);
  report.imageIdentity = process.env.MANGO_BUSINESS_LAB_IMAGE_IDENTITY || '';
  report.node = process.version;
  report.platform = process.platform;
  report.arch = process.arch;
  report.lockfileSha256 = sha256File(join(frontendRoot, 'pnpm-lock.yaml'));
  if (report.lockfileSha256 !== preparation.lockfileSha256) {
    throw new Error('Business Lab lockfile changed after the preparation phase');
  }

  report.networkInterfaces = assertLoopbackOnly();
  report.canaries = {
    dns: await dnsCanary(),
    https: await httpsCanary(),
  };
  if (!report.canaries.dns.blocked || !report.canaries.https.blocked) {
    throw new Error(`deny-all network canary failed: ${JSON.stringify(report.canaries)}`);
  }
  for (const proxyName of ['HTTP_PROXY', 'HTTPS_PROXY', 'ALL_PROXY']) {
    if (process.env[proxyName]) {
      throw new Error(`sealed environment retained proxy configuration: ${proxyName}`);
    }
  }

  assertGeneratedProjectBoundary(projectRoot, [process.env.MANGO_BUSINESS_LAB_FORBIDDEN_ROOT]);
  rmSync(join(frontendRoot, 'node_modules'), {
    recursive: true,
    force: true,
    maxRetries: 3,
    retryDelay: 200,
  });
  run(
    'pnpm',
    [
      'install',
      '--offline',
      '--frozen-lockfile',
      '--package-import-method=copy',
      '--child-concurrency=1',
      `--store-dir=${dependencyStore}`,
    ],
    { cwd: frontendRoot },
  );
  assertInstalledSymlinkBoundary(join(frontendRoot, 'node_modules'), [join(frontendRoot, 'packages')]);
  assertGeneratedProjectBoundary(projectRoot, [process.env.MANGO_BUSINESS_LAB_FORBIDDEN_ROOT]);

  const packageJson = readJson(join(frontendRoot, 'package.json'));
  const requiredScripts = ['format:check', 'lint', 'stylelint', 'typecheck', 'test:unit', 'build', 'check'];
  for (const script of requiredScripts) {
    if (!packageJson.scripts?.[script]) {
      throw new Error(`generated Business Lab is missing script: ${script}`);
    }
    run('pnpm', ['run', script], { cwd: frontendRoot });
  }

  run('pnpm', ['--dir', 'frontend', 'exec', 'mango', 'workspace', 'init']);
  const workspace = readJson(join(projectRoot, '.mango', 'workspace.json'));
  if (!/^mango_dev_[a-z0-9_]+_[0-9]{3}$/u.test(workspace.dbName || '')) {
    throw new Error(`workspace DB name is not isolated: ${workspace.dbName || ''}`);
  }
  if (!existsSync(join(projectRoot, '.mango', 'dev-workspace.env'))) {
    throw new Error('project-local CLI did not create .mango/dev-workspace.env');
  }
  report.workspace = safeWorkspaceEvidence(workspace);

  const devManifest = readJson(join(projectRoot, 'mango.dev.json'));
  startedApp = Object.entries(devManifest.apps || {}).find(([, app]) => app.type === 'vite')?.[0] || '';
  if (!startedApp) {
    throw new Error('generated Business Lab has no Vite app in mango.dev.json');
  }
  writeLocalFrontendOverride(startedApp);
  run('pnpm', ['--dir', 'frontend', 'exec', 'mango', 'dev', 'start', startedApp]);
  report.minimalShell = await waitForShell(`http://127.0.0.1:${workspace.frontendPort}/`);
  run('pnpm', ['--dir', 'frontend', 'exec', 'mango', 'dev', 'stop', startedApp]);
  startedApp = '';

  report.status = 'passed';
  report.completedAt = new Date().toISOString();
  report.qualityScripts = requiredScripts;
  report.sourceLeakCount = 0;
  report.successfulExternalConnectionCount = 0;
  writeJson(reportPath, report);
} catch (error) {
  report.error = error instanceof Error ? error.message : String(error);
  report.completedAt = new Date().toISOString();
  writeJson(reportPath, report);
  console.error(error instanceof Error ? error.stack || error.message : error);
  process.exitCode = 1;
} finally {
  if (startedApp) {
    run('pnpm', ['--dir', 'frontend', 'exec', 'mango', 'dev', 'stop', startedApp], { allowFailure: true });
  }
}
