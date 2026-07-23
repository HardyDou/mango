import assert from 'node:assert/strict';
import { createReadStream, existsSync, readFileSync, statSync } from 'node:fs';
import http from 'node:http';
import { extname, join, normalize, resolve } from 'node:path';
import test from 'node:test';

import { runBrowserProbe, runModuleDoctorCli } from '../../src/module-doctor.mjs';

const uiRoot = resolve(import.meta.dirname, '../../../..');
const shellDist = join(uiRoot, 'apps/mango-admin-shell/dist');
const shellProject = join(uiRoot, 'apps/mango-admin-shell');
const TOKEN = 'browser-e2e-diagnostic-token';

test('real Chromium loads mango-link page chunks and returns READY with frontend version', async () => {
  assert.ok(existsSync(join(shellDist, 'index.html')), 'build mango-admin-shell before running browser E2E');
  const builtHtml = readFileSync(join(shellDist, 'index.html'), 'utf8');
  assert.match(builtHtml, /http-equiv="Referrer-Policy" content="no-referrer"/);
  assert.doesNotMatch(builtHtml, /http-equiv="Content-Security-Policy"/);
  const frontend = await startStaticServer(shellDist);
  const backend = await startBackendServer();
  try {
    let output = '';
    const browserEvents = [];
    const exitCode = await runModuleDoctorCli(
      [
        'mango-link',
        '--app',
        'internal-admin',
        '--backend-url',
        backend.origin,
        '--frontend-url',
        frontend.origin,
        '--project-dir',
        shellProject,
        '--json',
      ],
      {
        env: { MANGO_DIAGNOSTIC_TOKEN: TOKEN },
        stdout: {
          write(chunk) {
            output += chunk;
          },
        },
        stderr: { write() {} },
        runBrowserProbe: (options) =>
          runBrowserProbe({
            ...options,
            onEvent: (event) => browserEvents.push(event),
          }),
      },
    );
    const result = JSON.parse(output);
    assert.equal(exitCode, 0, `${output}\n${browserEvents.join('\n')}`);
    assert.equal(result.status, 'READY');
    assert.equal(result.report.frontendVersion.value, '1.0.14');
    assert.equal(
      result.report.conditions.find((condition) => condition.id === 'frontend.pageRuntime').reasonCode,
      'PAGE_RUNTIME_READY',
    );
    assert.deepEqual(
      browserEvents.filter(
        (event) =>
          event.startsWith('console:error:') || event.startsWith('pageerror:') || event.startsWith('requestfailed:'),
      ),
      [],
      browserEvents.join('\n'),
    );
  } finally {
    await backend.close();
    await frontend.close();
  }
});

test('real Chromium reports a missing mango-link page as FAILED', async () => {
  const frontend = await startStaticServer(shellDist);
  const backend = await startBackendServer(['link/not-registered/index']);
  try {
    let output = '';
    const exitCode = await runModuleDoctorCli(
      [
        'mango-link',
        '--app',
        'internal-admin',
        '--backend-url',
        backend.origin,
        '--frontend-url',
        frontend.origin,
        '--project-dir',
        shellProject,
        '--json',
      ],
      {
        env: { MANGO_DIAGNOSTIC_TOKEN: TOKEN },
        stdout: {
          write(chunk) {
            output += chunk;
          },
        },
        stderr: { write() {} },
      },
    );
    const result = JSON.parse(output);
    assert.equal(exitCode, 1, output);
    assert.equal(result.status, 'FAILED');
    const frontendCondition = result.report.conditions.find((condition) => condition.id === 'frontend.pageRuntime');
    assert.equal(frontendCondition.status, 'FAIL');
    assert.equal(frontendCondition.evidence.pages[0].reasonCode, 'PAGE_NOT_REGISTERED');
  } finally {
    await backend.close();
    await frontend.close();
  }
});

async function startBackendServer(pageRequirements = ['link/categories/index', 'link/items/index']) {
  const server = http.createServer((request, response) => {
    const url = new URL(request.url, 'http://127.0.0.1');
    if (
      request.method !== 'GET' ||
      url.pathname !== '/actuator/mangoModules' ||
      request.headers.authorization !== `Bearer ${TOKEN}` ||
      url.searchParams.get('module') !== 'mango-link' ||
      url.searchParams.get('app') !== 'internal-admin' ||
      url.searchParams.get('profile') !== 'ADMIN_MODULE_RUNTIME_V1'
    ) {
      response.writeHead(403).end();
      return;
    }
    const pass = (id, reasonCode, evidence = {}) => ({
      id,
      status: 'PASS',
      required: true,
      reasonCode,
      evidence,
      observedAt: new Date().toISOString(),
      durationMs: 1,
      stale: false,
    });
    const snapshot = {
      schemaVersion: 1,
      profile: 'ADMIN_MODULE_RUNTIME_V1',
      reportScope: 'INSTANCE_OBSERVATION',
      service: 'browser-e2e',
      instanceId: 'fixture',
      observedAt: new Date().toISOString(),
      modules: [
        {
          moduleCode: 'mango-link',
          status: 'UNKNOWN',
          incompleteOptional: false,
          backendVersion: {
            value: '1.0.0',
            source: 'JAR_POM_PROPERTIES',
            status: 'PASS',
            reasonCode: 'VERSION_OBSERVED',
          },
          frontendVersion: {
            value: null,
            source: 'BROWSER_REPORT',
            status: 'UNKNOWN',
            reasonCode: 'FRONTEND_REPORT_PENDING',
          },
          expectedVersion: { value: null, source: 'NONE', status: 'UNKNOWN', reasonCode: 'NO_EXPECTATION_PROVIDER' },
          conditions: [
            pass('installation', 'MODULE_INSTALLED'),
            pass('persistence.flyway', 'FLYWAY_APPLIED'),
            pass('resource.materialization', 'CURRENT_DECLARATIONS_APPLIED'),
            pass('authorization.menuApi', 'AUTHORIZATION_MATERIALIZED', {
              pageRequirements,
            }),
            {
              id: 'frontend.pageRuntime',
              status: 'UNKNOWN',
              required: true,
              reasonCode: 'MISSING_CONTRIBUTOR',
              evidence: {},
              observedAt: new Date().toISOString(),
              durationMs: 0,
              stale: false,
            },
          ],
        },
      ],
    };
    response.setHeader('content-type', 'application/json');
    response.setHeader('cache-control', 'no-store');
    response.end(JSON.stringify(snapshot));
  });
  return listen(server);
}

async function startStaticServer(root) {
  const server = http.createServer((request, response) => {
    const requestPath = decodeURIComponent(new URL(request.url, 'http://127.0.0.1').pathname);
    const relativePath = requestPath === '/' ? 'index.html' : requestPath.replace(/^\//, '');
    let file = join(root, normalize(relativePath));
    if (!file.startsWith(root) || !existsSync(file) || !statSync(file).isFile()) {
      file = join(root, 'index.html');
    }
    response.setHeader('content-type', contentType(file));
    response.setHeader('cache-control', 'no-store');
    response.setHeader('referrer-policy', 'no-referrer');
    response.setHeader(
      'content-security-policy',
      "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:; connect-src 'self' http://127.0.0.1:*; object-src 'none'; base-uri 'none'; frame-ancestors 'none'",
    );
    createReadStream(file).pipe(response);
  });
  return listen(server);
}

function listen(server) {
  return new Promise((resolveListen, rejectListen) => {
    server.once('error', rejectListen);
    server.listen(0, '127.0.0.1', () => {
      const address = server.address();
      resolveListen({
        origin: `http://127.0.0.1:${address.port}`,
        close: () => new Promise((resolveClose) => server.close(resolveClose)),
      });
    });
  });
}

function contentType(file) {
  return (
    {
      '.css': 'text/css',
      '.html': 'text/html; charset=utf-8',
      '.ico': 'image/x-icon',
      '.js': 'text/javascript',
      '.json': 'application/json',
      '.woff': 'font/woff',
      '.woff2': 'font/woff2',
    }[extname(file)] || 'application/octet-stream'
  );
}
