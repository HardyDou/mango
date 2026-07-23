import assert from 'node:assert/strict';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';

import {
  createIsolatedBrowserEnvironment,
  mergeFrontendObservation,
  parseLoopbackBaseUrl,
  runModuleDoctorCli,
} from '../src/module-doctor.mjs';

const OBSERVED_AT = '2026-07-22T00:00:00Z';

function capture() {
  let value = '';
  return {
    stream: {
      write(chunk) {
        value += chunk;
      },
    },
    value() {
      return value;
    },
  };
}

function condition(id, status, reasonCode, evidence = {}) {
  return {
    id,
    status,
    required: true,
    reasonCode,
    evidence,
    observedAt: OBSERVED_AT,
    durationMs: 1,
    stale: false,
  };
}

function backendSnapshot(frontendStatus = 'UNKNOWN') {
  return {
    schemaVersion: 1,
    profile: 'ADMIN_MODULE_RUNTIME_V1',
    reportScope: 'INSTANCE_OBSERVATION',
    service: 'internal-admin',
    instanceId: 'test',
    observedAt: OBSERVED_AT,
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
        expectedVersion: {
          value: null,
          source: 'NONE',
          status: 'UNKNOWN',
          reasonCode: 'NO_EXPECTATION_PROVIDER',
        },
        conditions: [
          condition('installation', 'PASS', 'MODULE_INSTALLED'),
          condition('persistence.flyway', 'PASS', 'FLYWAY_APPLIED'),
          condition('resource.materialization', 'PASS', 'CURRENT_DECLARATIONS_APPLIED'),
          condition('authorization.menuApi', 'PASS', 'AUTHORIZATION_MATERIALIZED', {
            pageRequirements: ['link/items/index'],
          }),
          condition('frontend.pageRuntime', frontendStatus, 'MISSING_CONTRIBUTOR'),
        ],
      },
    ],
  };
}

function browserResult() {
  return {
    schemaVersion: 1,
    nonce: '0123456789abcdefghijklmn',
    moduleCode: 'mango-link',
    observedAt: OBSERVED_AT,
    condition: {
      ...condition('frontend.pageRuntime', 'PASS', 'PAGE_RUNTIME_READY', {
        expectedPageCount: 1,
        failedPageCount: 0,
        pages: [{ component: 'link/items/index', status: 'PASS', reasonCode: 'PAGE_REGISTERED' }],
      }),
      durationMs: 10,
    },
    frontendVersion: {
      value: '1.0.14',
      source: 'PAGE_REGISTRAR',
      status: 'PASS',
      reasonCode: 'FRONTEND_VERSION_OBSERVED',
    },
  };
}

function projectDirectory() {
  const directory = mkdtempSync(join(tmpdir(), 'mango-doctor-test-'));
  writeFileSync(join(directory, 'package.json'), '{}');
  return directory;
}

function args(projectDir, json = true) {
  return [
    'mango-link',
    '--app',
    'internal-admin',
    '--backend-url',
    'http://127.0.0.1:18081',
    '--frontend-url',
    'http://127.0.0.1:5176',
    '--project-dir',
    projectDir,
    ...(json ? ['--json'] : []),
  ];
}

async function invoke({ snapshot = backendSnapshot(), browser = browserResult(), json = true } = {}) {
  const stdout = capture();
  const stderr = capture();
  const exitCode = await runModuleDoctorCli(args(projectDirectory(), json), {
    env: { MANGO_DIAGNOSTIC_TOKEN: 'test-token' },
    stdout: stdout.stream,
    stderr: stderr.stream,
    fetchBackend: async () => snapshot,
    runBrowserProbe: async () => browser,
  });
  return { exitCode, stdout: stdout.value(), stderr: stderr.value() };
}

test('loopback URL validation rejects localhost, paths, credentials and remote hosts', () => {
  assert.equal(parseLoopbackBaseUrl('http://127.0.0.1:18081', '--backend-url'), 'http://127.0.0.1:18081');
  for (const value of [
    'http://localhost:18081',
    'http://127.0.0.1:18081/path',
    'http://user@127.0.0.1:18081',
    'https://127.0.0.1:18081',
    'http://10.0.0.1:18081',
  ]) {
    assert.throws(() => parseLoopbackBaseUrl(value, '--backend-url'));
  }
});

test('frontend runtime observation replaces the backend missing contributor and reaggregates', () => {
  const browser = browserResult();
  const report = mergeFrontendObservation(backendSnapshot().modules[0], browser.condition, browser.frontendVersion);
  assert.equal(report.status, 'READY');
  assert.equal(report.frontendVersion.value, '1.0.14');
  assert.equal(report.conditions.filter((item) => item.id === 'frontend.pageRuntime').length, 1);
});

test('valid injected backend and browser protocol produces READY', async () => {
  const result = await invoke();
  assert.equal(result.exitCode, 0);
  assert.equal(result.stderr, '');
  assert.equal(JSON.parse(result.stdout).status, 'READY');
});

const backendSchemaCases = [
  ['missing required condition', (snapshot) => snapshot.modules[0].conditions.pop()],
  [
    'duplicate required condition',
    (snapshot) => {
      snapshot.modules[0].conditions[4].id = 'installation';
    },
  ],
  [
    'required false',
    (snapshot) => {
      snapshot.modules[0].conditions[0].required = false;
    },
  ],
  [
    'invalid condition status',
    (snapshot) => {
      snapshot.modules[0].conditions[0].status = 'READY';
    },
  ],
  [
    'non-boolean stale',
    (snapshot) => {
      snapshot.modules[0].conditions[0].stale = 'false';
    },
  ],
  [
    'unsafe reason code',
    (snapshot) => {
      snapshot.modules[0].conditions[0].reasonCode = 'module installed';
    },
  ],
  [
    'invalid version source',
    (snapshot) => {
      snapshot.modules[0].backendVersion.source = 'jar source';
    },
  ],
  [
    'invalid unknown version value',
    (snapshot) => {
      snapshot.modules[0].expectedVersion.value = '1.0.0';
    },
  ],
  [
    'oversized evidence',
    (snapshot) => {
      snapshot.modules[0].conditions[0].evidence = { value: 'x'.repeat(17 * 1024) };
    },
  ],
  [
    'over-deep evidence',
    (snapshot) => {
      snapshot.modules[0].conditions[0].evidence = { a: { b: { c: { d: { e: 1 } } } } };
    },
  ],
];

for (const [name, mutate] of backendSchemaCases) {
  test(`backend schema fails closed for ${name}`, async () => {
    const snapshot = backendSnapshot();
    mutate(snapshot);
    const result = await invoke({ snapshot });
    assert.equal(result.exitCode, 3);
    assert.equal(result.stderr, '');
    const output = JSON.parse(result.stdout);
    assert.equal(output.status, 'UNKNOWN');
    assert.equal(output.error.reasonCode, 'BACKEND_SCHEMA_UNSUPPORTED');
    assert.equal(output.report, undefined);
  });
}

const browserSchemaCases = [
  [
    'required false',
    (browser) => {
      browser.condition.required = false;
    },
  ],
  [
    'stale result',
    (browser) => {
      browser.condition.stale = true;
    },
  ],
  [
    'invalid version source',
    (browser) => {
      browser.frontendVersion.source = 'PACKAGE_JSON';
    },
  ],
  [
    'page count mismatch',
    (browser) => {
      browser.condition.evidence.pages = [];
    },
  ],
  [
    'invalid page evidence',
    (browser) => {
      browser.condition.evidence.pages[0].component = 'https://outside.example/page';
    },
  ],
];

for (const [name, mutate] of browserSchemaCases) {
  test(`browser schema degrades to UNKNOWN for ${name}`, async () => {
    const browser = browserResult();
    mutate(browser);
    const result = await invoke({ browser });
    assert.equal(result.exitCode, 3);
    assert.equal(result.stderr, '');
    const output = JSON.parse(result.stdout);
    const frontend = output.report.conditions.find((item) => item.id === 'frontend.pageRuntime');
    assert.equal(output.status, 'UNKNOWN');
    assert.equal(frontend.status, 'UNKNOWN');
    assert.equal(frontend.reasonCode, 'BROWSER_SCHEMA_UNSUPPORTED');
  });
}

test('missing Playwright returns a safe installation hint in JSON without installing anything', async () => {
  const stdout = capture();
  const stderr = capture();
  const exitCode = await runModuleDoctorCli(args(projectDirectory()), {
    env: { MANGO_DIAGNOSTIC_TOKEN: 'test-token' },
    stdout: stdout.stream,
    stderr: stderr.stream,
    fetchBackend: async () => backendSnapshot(),
  });
  assert.equal(exitCode, 3);
  assert.equal(stderr.value(), '');
  const output = JSON.parse(stdout.value());
  const frontend = output.report.conditions.find((item) => item.id === 'frontend.pageRuntime');
  assert.equal(frontend.reasonCode, 'PLAYWRIGHT_UNAVAILABLE');
  assert.match(frontend.evidence.hint, /pnpm add --save-dev @playwright\/test/);
});

test('text output prints the safe installation hint', async () => {
  const stdout = capture();
  const stderr = capture();
  const exitCode = await runModuleDoctorCli(args(projectDirectory(), false), {
    env: { MANGO_DIAGNOSTIC_TOKEN: 'test-token' },
    stdout: stdout.stream,
    stderr: stderr.stream,
    fetchBackend: async () => backendSnapshot(),
  });
  assert.equal(exitCode, 3);
  assert.equal(stderr.value(), '');
  assert.match(stdout.value(), /hint: Install Playwright in --project-dir explicitly/);
});

test('browser process environment is an allowlist and excludes diagnostic and cloud secrets', () => {
  const isolated = createIsolatedBrowserEnvironment({
    PATH: '/usr/bin',
    HOME: '/tmp/test-home',
    TMPDIR: '/tmp',
    PLAYWRIGHT_BROWSERS_PATH: '/tmp/browsers',
    MANGO_DIAGNOSTIC_TOKEN: 'diagnostic-secret',
    AWS_SECRET_ACCESS_KEY: 'cloud-secret',
    BUSINESS_PRIVATE_KEY: 'business-secret',
  });
  assert.deepEqual(isolated, {
    HOME: '/tmp/test-home',
    PATH: '/usr/bin',
    PLAYWRIGHT_BROWSERS_PATH: '/tmp/browsers',
    TMPDIR: '/tmp',
  });
  assert.equal('MANGO_DIAGNOSTIC_TOKEN' in isolated, false);
  assert.equal('AWS_SECRET_ACCESS_KEY' in isolated, false);
  assert.equal('BUSINESS_PRIVATE_KEY' in isolated, false);
});
