#!/usr/bin/env node
import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from '@playwright/test';

const uiRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = resolve(uiRoot, '..');
const runtimeVolume = readArgument('--runtime-volume=');
const image = readArgument('--image=') || 'mango/frontend-quality:node22-pnpm11.14';
const gitSha = readArgument('--git-sha=');
const gitTree = readArgument('--git-tree=');
const containerName = `mango-business-lab-browser-${Date.now()}-${process.pid}`;
const reportRoot = join(repoRoot, '.runtime', 'frontend-quality', 'business-lab');
const reportPath = join(reportRoot, 'browser-contract.json');
const screenshotPath = join(reportRoot, 'browser-contract.png');

if (!runtimeVolume) throw new Error('--runtime-volume is required');

function readArgument(prefix) {
  return process.argv.find((argument) => argument.startsWith(prefix))?.slice(prefix.length) || '';
}

function docker(args, capture = false) {
  const result = spawnSync('docker', args, {
    cwd: repoRoot,
    encoding: 'utf8',
    stdio: capture ? 'pipe' : 'inherit',
  });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(`docker ${args.join(' ')} failed (${result.status}): ${result.stderr || ''}`);
  }
  return result.stdout?.trim() || '';
}

async function waitForURL(url, timeoutMs = 60_000) {
  const deadline = Date.now() + timeoutMs;
  let lastError = '';
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url);
      if (response.ok) return;
      lastError = `HTTP ${response.status}`;
    } catch (error) {
      lastError = error instanceof Error ? error.message : String(error);
    }
    await new Promise((resolveWait) => setTimeout(resolveWait, 500));
  }
  const containerLogs = docker(['logs', containerName], true);
  throw new Error(`generated frontend did not become ready: ${lastError}\n${containerLogs}`);
}

mkdirSync(reportRoot, { recursive: true });
const report = {
  schemaVersion: 1,
  status: 'failed',
  generatedAt: new Date().toISOString(),
  gitSha,
  gitTree,
  image,
  browser: 'chrome',
};
let browser;

try {
  docker([
    'run',
    '--rm',
    '--detach',
    '--name',
    containerName,
    '--publish',
    '127.0.0.1::4179',
    '--volume',
    `${runtimeVolume}:/runtime`,
    '--workdir',
    '/runtime/projects/frontend-standards-business-lab/frontend',
    image,
    'node',
    'node_modules/vite/bin/vite.js',
    '--host',
    '0.0.0.0',
    '--port',
    '4179',
    '--strictPort',
  ]);
  const published = docker(['port', containerName, '4179/tcp'], true);
  const port = published.match(/:(\d+)$/u)?.[1];
  if (!port) throw new Error(`cannot resolve generated frontend port from: ${published}`);
  const baseURL = `http://127.0.0.1:${port}`;
  await waitForURL(`${baseURL}/runtime-config.json`);

  browser = await chromium.launch({ channel: 'chrome', headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.route('**/favicon.ico', (route) => route.fulfill({ status: 204, body: '' }));
  const consoleErrors = [];
  const requestFailures = [];
  page.on('console', (message) => {
    if (message.type() === 'error') consoleErrors.push(message.text());
  });
  page.on('requestfailed', (request) => {
    requestFailures.push(`${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`);
  });
  await page.goto(`${baseURL}/runtime-config.json`);
  const contract = await page.evaluate(async () => {
    const apiModule = await import('/packages/orders-api/src/api.ts');
    const pageModule = await import('/packages/orders/src/views/orders/sales-order/index.vue');
    const requests = [];
    const client = {
      async request(request) {
        requests.push({
          method: request.method,
          url: request.url,
          query: request.query || null,
          body: request.body || null,
        });
        if (request.url.endsWith('/create')) return 'SO-NEW';
        if (request.url.endsWith('/page')) {
          return { records: [{ id: 'SO-1', name: '首张订单' }], total: 1, page: 1, size: 20, pages: 1 };
        }
        if (request.url.endsWith('/detail')) return { id: 'SO-1', name: '首张订单' };
        return true;
      },
    };
    const api = apiModule.createSalesOrderApi(client);
    const results = {
      createdId: await api.create({ name: '新订单' }),
      updated: await api.update({ id: 'SO-1', name: '已更新订单' }),
      deleted: await api.delete({ id: 'SO-1' }),
      page: await api.page({ page: 1, size: 20, name: '首张' }),
      detail: await api.detail('SO-1'),
    };
    return {
      pageModuleLoaded: Boolean(pageModule.default),
      apiFactoryLoaded: typeof apiModule.createSalesOrderApi === 'function',
      requests,
      results,
    };
  });

  assert.equal(contract.pageModuleLoaded, true);
  assert.equal(contract.apiFactoryLoaded, true);
  assert.deepEqual(contract.requests, [
    { method: 'POST', url: '/orders/sales-orders/create', query: null, body: { name: '新订单' } },
    {
      method: 'POST',
      url: '/orders/sales-orders/update',
      query: null,
      body: { id: 'SO-1', name: '已更新订单' },
    },
    { method: 'POST', url: '/orders/sales-orders/delete', query: null, body: { id: 'SO-1' } },
    {
      method: 'GET',
      url: '/orders/sales-orders/page',
      query: { page: 1, size: 20, name: '首张' },
      body: null,
    },
    { method: 'GET', url: '/orders/sales-orders/detail', query: { id: 'SO-1' }, body: null },
  ]);
  assert.deepEqual(contract.results, {
    createdId: 'SO-NEW',
    updated: true,
    deleted: true,
    page: { records: [{ id: 'SO-1', name: '首张订单' }], total: 1, page: 1, size: 20, pages: 1 },
    detail: { id: 'SO-1', name: '首张订单' },
  });
  assert.deepEqual(consoleErrors, []);
  assert.deepEqual(requestFailures, []);

  await page.setContent(`
    <main style="font: 16px system-ui; padding: 32px">
      <h1>Mango Business Lab Browser Contract</h1>
      <p>Generated orders page module loaded.</p>
      <p>Five typed API operations matched the expected transport contract.</p>
    </main>
  `);
  await page.screenshot({ path: screenshotPath, fullPage: true });
  report.status = 'passed';
  report.completedAt = new Date().toISOString();
  report.baseURL = baseURL;
  report.consoleErrors = consoleErrors;
  report.requestFailures = requestFailures;
  report.contract = contract;
  report.screenshot = 'browser-contract.png';
  writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(`Mango Business Lab browser contract PASS: ${reportPath}`);
} catch (error) {
  report.completedAt = new Date().toISOString();
  report.error = error instanceof Error ? error.message : String(error);
  writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);
  throw error;
} finally {
  await browser?.close();
  spawnSync('docker', ['rm', '--force', containerName], { stdio: 'ignore' });
}
