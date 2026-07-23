import { randomBytes } from 'node:crypto';
import { existsSync } from 'node:fs';
import http from 'node:http';
import { createRequire } from 'node:module';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const PROFILE = 'ADMIN_MODULE_RUNTIME_V1';
const ENDPOINT_PATH = '/actuator/mangoModules';
const CONDITION_ID = 'frontend.pageRuntime';
const REQUIRED_CONDITION_IDS = [
  'installation',
  'persistence.flyway',
  'resource.materialization',
  'authorization.menuApi',
  CONDITION_ID,
];
const CONDITION_STATUSES = new Set(['PASS', 'WARN', 'FAIL', 'UNKNOWN', 'SKIPPED']);
const MODULE_STATUSES = new Set(['READY', 'DEGRADED', 'FAILED', 'UNKNOWN', 'DISABLED']);
const VERSION_STATUSES = new Set(['PASS', 'UNKNOWN']);
const MAX_BACKEND_BYTES = 1024 * 1024;
const MAX_CALLBACK_BYTES = 64 * 1024;
const MAX_EVIDENCE_BYTES = 16 * 1024;
const MAX_TEXT_LENGTH = 500;
const MAX_BACKEND_DURATION_MS = 24 * 60 * 60 * 1000;
const MAX_BROWSER_DURATION_MS = 60_000;
const SAFE_REASON_CODE = /^[A-Z][A-Z0-9_]{0,79}$/;
const SAFE_VERSION_SOURCE = /^[A-Z][A-Z0-9_.-]{0,79}$/;
const SAFE_MODULE_CODE = /^[a-z0-9][a-z0-9-]{0,79}$/;
const SAFE_PAGE_REQUIREMENT = /^[A-Za-z0-9_./-]{1,200}$/;

const SAFE_DIAGNOSTIC_HINTS = Object.freeze({
  PLAYWRIGHT_UNAVAILABLE:
    'Install Playwright in --project-dir explicitly, for example: pnpm add --save-dev @playwright/test',
  CHROMIUM_UNAVAILABLE: 'Install the project Chromium explicitly, for example: pnpm exec playwright install chromium',
});

class ModuleDoctorError extends Error {
  constructor(reasonCode, message, exitCode = 3) {
    super(message);
    this.reasonCode = reasonCode;
    this.exitCode = exitCode;
  }
}

export async function runModuleDoctorCli(argv, dependencies = {}) {
  const stdout = dependencies.stdout || process.stdout;
  const stderr = dependencies.stderr || process.stderr;
  let options;
  try {
    options = parseModuleDoctorArgs(argv, dependencies.cwd || process.cwd());
    const token = (dependencies.env || process.env).MANGO_DIAGNOSTIC_TOKEN;
    if (!token || !token.trim()) {
      throw new ModuleDoctorError(
        'DIAGNOSTIC_TOKEN_MISSING',
        'MANGO_DIAGNOSTIC_TOKEN must contain a bearer access token with diagnostic:read permission',
        2,
      );
    }
    const backendSnapshot = await (dependencies.fetchBackend || fetchBackendSnapshot)({
      ...options,
      token: token.trim(),
    });
    validateBackendSnapshot(backendSnapshot, options.moduleCode);
    const backendReport = backendSnapshot.modules.find((module) => module.moduleCode === options.moduleCode);
    if (!backendReport) {
      throw new ModuleDoctorError('MODULE_REPORT_MISSING', 'backend response did not contain the requested module');
    }
    const pageRequirements = extractPageRequirements(backendReport);
    let frontendCondition;
    let frontendVersion = unknownVersion('FRONTEND_VERSION_UNKNOWN');
    if (pageRequirements.length === 0) {
      frontendCondition = unknownFrontendCondition('PAGE_REQUIREMENTS_MISSING');
    } else {
      try {
        const browserResult = await (dependencies.runBrowserProbe || runBrowserProbe)({
          ...options,
          pageRequirements,
        });
        validateBrowserResult(browserResult, options.moduleCode);
        frontendCondition = browserResult.condition;
        frontendVersion = browserResult.frontendVersion;
      } catch (error) {
        const reasonCode = error instanceof ModuleDoctorError ? error.reasonCode : 'BROWSER_PROBE_UNAVAILABLE';
        frontendCondition = unknownFrontendCondition(reasonCode, diagnosticHint(reasonCode));
      }
    }
    const report = mergeFrontendObservation(backendReport, frontendCondition, frontendVersion);
    const exitCode = exitCodeForReport(report, options.strict);
    writeResult(stdout, stderr, options, {
      schemaVersion: 1,
      command: 'mango module doctor',
      moduleCode: options.moduleCode,
      status: report.status,
      exitCode,
      backend: {
        service: backendSnapshot.service,
        instanceId: backendSnapshot.instanceId,
        observedAt: backendSnapshot.observedAt,
      },
      report,
    });
    return exitCode;
  } catch (error) {
    const failure =
      error instanceof ModuleDoctorError
        ? error
        : new ModuleDoctorError('MODULE_DOCTOR_FAILED', 'module diagnosis failed');
    const fallbackOptions = options || { json: argv.includes('--json'), moduleCode: argv[0] || null };
    writeResult(stdout, stderr, fallbackOptions, {
      schemaVersion: 1,
      command: 'mango module doctor',
      moduleCode: fallbackOptions.moduleCode || null,
      status: 'UNKNOWN',
      exitCode: failure.exitCode,
      error: {
        reasonCode: failure.reasonCode,
        message: failure.message,
      },
    });
    return failure.exitCode;
  }
}

export function parseLoopbackBaseUrl(value, optionName) {
  let url;
  try {
    url = new URL(value);
  } catch {
    throw new ModuleDoctorError('INVALID_ARGUMENT', `${optionName} must be an absolute loopback URL`, 2);
  }
  if (
    url.protocol !== 'http:' ||
    !['127.0.0.1', '[::1]'].includes(url.hostname) ||
    url.username ||
    url.password ||
    url.pathname !== '/' ||
    url.search ||
    url.hash
  ) {
    throw new ModuleDoctorError(
      'INVALID_ARGUMENT',
      `${optionName} must be an http://127.0.0.1 or http://[::1] base origin without path, credentials, query or fragment`,
      2,
    );
  }
  return url.origin;
}

export function mergeFrontendObservation(backendReport, frontendCondition, frontendVersion) {
  const conditions = backendReport.conditions
    .filter((condition) => condition.id !== CONDITION_ID)
    .concat(frontendCondition)
    .sort((left, right) => left.id.localeCompare(right.id));
  return {
    ...backendReport,
    frontendVersion,
    conditions,
    status: aggregateStatus(conditions),
  };
}

function parseModuleDoctorArgs(argv, cwd) {
  const values = {};
  const flags = new Set();
  const positional = [];
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index];
    if (value === '--json' || value === '--strict') {
      flags.add(value);
      continue;
    }
    if (['--app', '--backend-url', '--frontend-url', '--project-dir'].includes(value)) {
      const next = argv[index + 1];
      if (!next || next.startsWith('--')) {
        throw new ModuleDoctorError('INVALID_ARGUMENT', `${value} requires a value`, 2);
      }
      values[value] = next;
      index += 1;
      continue;
    }
    if (value.startsWith('--')) {
      throw new ModuleDoctorError('INVALID_ARGUMENT', `unknown option: ${value}`, 2);
    }
    positional.push(value);
  }
  if (positional.length !== 1 || positional[0] !== 'mango-link') {
    throw new ModuleDoctorError(
      'INVALID_ARGUMENT',
      'the first release supports exactly: mango module doctor mango-link',
      2,
    );
  }
  if (!values['--app'] || !/^[A-Za-z0-9][A-Za-z0-9_-]{0,79}$/.test(values['--app'])) {
    throw new ModuleDoctorError('INVALID_ARGUMENT', '--app is required and must be a safe application code', 2);
  }
  if (!values['--backend-url'] || !values['--frontend-url']) {
    throw new ModuleDoctorError('INVALID_ARGUMENT', '--backend-url and --frontend-url are required', 2);
  }
  const projectDir = resolve(cwd, values['--project-dir'] || '.');
  if (!existsSync(projectDir)) {
    throw new ModuleDoctorError('INVALID_ARGUMENT', `--project-dir does not exist: ${projectDir}`, 2);
  }
  return {
    moduleCode: positional[0],
    appCode: values['--app'],
    backendOrigin: parseLoopbackBaseUrl(values['--backend-url'], '--backend-url'),
    frontendOrigin: parseLoopbackBaseUrl(values['--frontend-url'], '--frontend-url'),
    projectDir,
    json: flags.has('--json'),
    strict: flags.has('--strict'),
  };
}

async function fetchBackendSnapshot(options) {
  const endpoint = new URL(ENDPOINT_PATH, options.backendOrigin);
  endpoint.searchParams.set('module', options.moduleCode);
  endpoint.searchParams.set('app', options.appCode);
  endpoint.searchParams.set('profile', PROFILE);
  let response;
  try {
    response = await fetch(endpoint, {
      method: 'GET',
      headers: {
        authorization: `Bearer ${options.token}`,
        accept: 'application/json',
      },
      redirect: 'error',
      signal: AbortSignal.timeout(5_000),
    });
  } catch {
    throw new ModuleDoctorError('BACKEND_UNAVAILABLE', 'module diagnostic endpoint is unavailable');
  }
  if (response.status === 401 || response.status === 403) {
    throw new ModuleDoctorError('DIAGNOSTIC_ACCESS_DENIED', `module diagnostic endpoint returned ${response.status}`);
  }
  if (!response.ok) {
    throw new ModuleDoctorError('BACKEND_PROTOCOL_ERROR', `module diagnostic endpoint returned ${response.status}`);
  }
  const contentLength = Number(response.headers.get('content-length') || 0);
  if (contentLength > MAX_BACKEND_BYTES) {
    throw new ModuleDoctorError('BACKEND_RESPONSE_TOO_LARGE', 'module diagnostic response exceeded the size limit');
  }
  const text = await response.text();
  if (Buffer.byteLength(text) > MAX_BACKEND_BYTES) {
    throw new ModuleDoctorError('BACKEND_RESPONSE_TOO_LARGE', 'module diagnostic response exceeded the size limit');
  }
  let snapshot;
  try {
    snapshot = JSON.parse(text);
  } catch {
    throw new ModuleDoctorError('BACKEND_PROTOCOL_ERROR', 'module diagnostic endpoint returned invalid JSON');
  }
  return snapshot;
}

function validateBackendSnapshot(snapshot, requestedModuleCode) {
  if (
    !isRecord(snapshot) ||
    snapshot.schemaVersion !== 1 ||
    snapshot.profile !== PROFILE ||
    snapshot.reportScope !== 'INSTANCE_OBSERVATION' ||
    !isBoundedText(snapshot.service, 200) ||
    !isBoundedText(snapshot.instanceId, 200) ||
    !isIsoInstant(snapshot.observedAt) ||
    !Array.isArray(snapshot.modules) ||
    snapshot.modules.length !== 1 ||
    !validateBackendModule(snapshot.modules[0], requestedModuleCode)
  ) {
    throw new ModuleDoctorError('BACKEND_SCHEMA_UNSUPPORTED', 'module diagnostic response schema is unsupported');
  }
}

function validateBackendModule(module, requestedModuleCode) {
  if (
    !isRecord(module) ||
    module.moduleCode !== requestedModuleCode ||
    !SAFE_MODULE_CODE.test(module.moduleCode) ||
    !MODULE_STATUSES.has(module.status) ||
    typeof module.incompleteOptional !== 'boolean' ||
    !validateVersion(module.backendVersion) ||
    !validateVersion(module.frontendVersion) ||
    !validateVersion(module.expectedVersion) ||
    !Array.isArray(module.conditions) ||
    module.conditions.length !== REQUIRED_CONDITION_IDS.length
  ) {
    return false;
  }
  const ids = module.conditions.map((condition) => (isRecord(condition) ? condition.id : undefined));
  if (new Set(ids).size !== REQUIRED_CONDITION_IDS.length) {
    return false;
  }
  return (
    REQUIRED_CONDITION_IDS.every((conditionId) => ids.includes(conditionId)) &&
    module.conditions.every((condition) => validateBackendCondition(condition))
  );
}

function validateBackendCondition(condition) {
  return (
    isRecord(condition) &&
    REQUIRED_CONDITION_IDS.includes(condition.id) &&
    CONDITION_STATUSES.has(condition.status) &&
    condition.required === true &&
    isReasonCode(condition.reasonCode) &&
    validateEvidence(condition.evidence) &&
    isIsoInstant(condition.observedAt) &&
    isBoundedDuration(condition.durationMs, MAX_BACKEND_DURATION_MS) &&
    typeof condition.stale === 'boolean'
  );
}

function validateVersion(version) {
  if (
    !isRecord(version) ||
    !VERSION_STATUSES.has(version.status) ||
    !isReasonCode(version.reasonCode) ||
    typeof version.source !== 'string' ||
    !SAFE_VERSION_SOURCE.test(version.source)
  ) {
    return false;
  }
  if (version.status === 'PASS') {
    return isBoundedText(version.value, 100);
  }
  return version.value === null;
}

function validateEvidence(evidence) {
  if (!isRecord(evidence)) {
    return false;
  }
  try {
    if (Buffer.byteLength(JSON.stringify(evidence)) > MAX_EVIDENCE_BYTES) {
      return false;
    }
  } catch {
    return false;
  }
  if (!isBoundedJson(evidence, 0)) {
    return false;
  }
  if ('pageRequirements' in evidence) {
    return (
      Array.isArray(evidence.pageRequirements) &&
      evidence.pageRequirements.length <= 32 &&
      evidence.pageRequirements.every((value) => typeof value === 'string' && SAFE_PAGE_REQUIREMENT.test(value)) &&
      new Set(evidence.pageRequirements).size === evidence.pageRequirements.length
    );
  }
  return true;
}

function extractPageRequirements(report) {
  const values = report.conditions
    .filter((condition) => condition.id === 'authorization.menuApi' || condition.id === 'resource.materialization')
    .flatMap((condition) =>
      Array.isArray(condition.evidence?.pageRequirements) ? condition.evidence.pageRequirements : [],
    )
    .filter((value) => typeof value === 'string' && SAFE_PAGE_REQUIREMENT.test(value));
  return [...new Set(values)].sort().slice(0, 32);
}

export async function runBrowserProbe(options) {
  const playwright = await loadProjectPlaywright(options.projectDir);
  const callback = await createCallbackServer(options.frontendOrigin);
  const nonce = randomBytes(16).toString('base64url');
  callback.bindNonce(nonce);
  const challenge = {
    schemaVersion: 1,
    nonce,
    expiresAt: Date.now() + 30_000,
    frontendOrigin: options.frontendOrigin,
    callbackUrl: callback.url(nonce),
    moduleCode: options.moduleCode,
    pageRequirements: options.pageRequirements,
  };
  const encoded = Buffer.from(JSON.stringify(challenge)).toString('base64url');
  const diagnosticUrl = `${options.frontendOrigin}/#mango-module-doctor=${encoded}`;
  let browser;
  let page;
  try {
    try {
      browser = await playwright.chromium.launch({
        headless: true,
        env: createIsolatedBrowserEnvironment(options.env || process.env),
      });
    } catch {
      throw new ModuleDoctorError(
        'CHROMIUM_UNAVAILABLE',
        'project Playwright Chromium is unavailable; install it explicitly with your project package manager',
      );
    }
    const context = await browser.newContext({
      acceptDownloads: false,
      permissions: [],
      serviceWorkers: 'block',
      storageState: { cookies: [], origins: [] },
    });
    await context.route('**/*', async (route) => {
      let origin;
      try {
        origin = new URL(route.request().url()).origin;
      } catch {
        await route.abort('blockedbyclient');
        return;
      }
      if (origin === options.frontendOrigin || origin === callback.origin()) {
        await route.continue();
      } else {
        await route.abort('blockedbyclient');
      }
    });
    page = await context.newPage();
    if (options.onEvent) {
      page.on('console', (message) => options.onEvent(`console:${message.type()}:${message.text()}`));
      page.on('pageerror', (error) => options.onEvent(`pageerror:${error.message}`));
      page.on('requestfailed', (request) => {
        if (request.url() !== challenge.callbackUrl) {
          options.onEvent(`requestfailed:${request.url()}:${request.failure()?.errorText || 'unknown'}`);
        }
      });
    }
    await page.goto(diagnosticUrl, {
      waitUntil: 'domcontentloaded',
      timeout: 10_000,
    });
    const result = await callback.result;
    await page.waitForFunction(() => globalThis.__MANGO_MODULE_DIAGNOSTIC_STATE__ === 'COMPLETE', undefined, {
      timeout: 2_000,
    });
    return result;
  } catch (error) {
    if (options.onEvent && page) {
      const state = await page.evaluate(() => globalThis.__MANGO_MODULE_DIAGNOSTIC_STATE__).catch(() => undefined);
      options.onEvent(`bridge-state:${state || 'UNAVAILABLE'}`);
      const location = await page
        .evaluate(() => ({
          origin: globalThis.location.origin,
          hash: globalThis.location.hash,
        }))
        .catch(() => undefined);
      if (location) {
        const expectedHash = new URL(diagnosticUrl).hash;
        options.onEvent(
          `challenge-location:originMatch=${location.origin === options.frontendOrigin},hashMatch=${location.hash === expectedHash},challengePrefix=${location.hash.startsWith('#mango-module-doctor=')},hashLength=${location.hash.length},expectedLength=${expectedHash.length}`,
        );
      }
    }
    if (error instanceof ModuleDoctorError) {
      throw error;
    }
    throw new ModuleDoctorError('BROWSER_PROBE_FAILED', 'browser runtime probe did not complete');
  } finally {
    await browser?.close().catch(() => undefined);
    await callback.close();
  }
}

async function loadProjectPlaywright(projectDir) {
  const packageFile = resolve(projectDir, 'package.json');
  if (!existsSync(packageFile)) {
    throw new ModuleDoctorError(
      'PLAYWRIGHT_UNAVAILABLE',
      '--project-dir must contain package.json with Playwright installed',
    );
  }
  const projectRequire = createRequire(packageFile);
  let entry;
  for (const packageName of ['playwright', '@playwright/test']) {
    try {
      entry = projectRequire.resolve(packageName);
      break;
    } catch {
      // Try the next supported project dependency.
    }
  }
  if (!entry) {
    throw new ModuleDoctorError(
      'PLAYWRIGHT_UNAVAILABLE',
      'Playwright is not installed in --project-dir; add playwright or @playwright/test explicitly',
    );
  }
  const loaded = await import(pathToFileURL(entry).href);
  const playwright = loaded.chromium ? loaded : loaded.default;
  if (!playwright?.chromium) {
    throw new ModuleDoctorError('PLAYWRIGHT_UNAVAILABLE', 'the project Playwright package does not export Chromium');
  }
  return playwright;
}

async function createCallbackServer(frontendOrigin) {
  let expectedNonce;
  let consumed = false;
  let settleResult;
  let settleError;
  const result = new Promise((resolveResult, rejectResult) => {
    settleResult = resolveResult;
    settleError = rejectResult;
  });
  const timeout = setTimeout(
    () => settleError(new ModuleDoctorError('BROWSER_CALLBACK_TIMEOUT', 'browser diagnostic callback timed out')),
    15_000,
  );
  const server = http.createServer((request, response) => {
    const address = server.address();
    const expectedHost = address && typeof address === 'object' ? `127.0.0.1:${address.port}` : '';
    const allowOrigin = request.headers.origin === frontendOrigin;
    response.setHeader('cache-control', 'no-store');
    if (allowOrigin) {
      response.setHeader('access-control-allow-origin', frontendOrigin);
      response.setHeader('vary', 'Origin');
    }
    if (request.method === 'OPTIONS') {
      if (!allowOrigin || request.headers.host !== expectedHost) {
        response.writeHead(403).end();
        return;
      }
      response.setHeader('access-control-allow-methods', 'POST');
      response.setHeader('access-control-allow-headers', 'content-type');
      response.writeHead(204).end();
      return;
    }
    if (
      request.method !== 'POST' ||
      consumed ||
      !expectedNonce ||
      request.url !== `/mango-module-doctor/${expectedNonce}` ||
      request.headers.host !== expectedHost ||
      !allowOrigin ||
      request.headers['content-type']?.toLowerCase() !== 'text/plain;charset=utf-8' ||
      !isLoopbackRemote(request.socket.remoteAddress)
    ) {
      response.writeHead(403).end();
      return;
    }
    consumed = true;
    const chunks = [];
    let size = 0;
    request.on('data', (chunk) => {
      size += chunk.length;
      if (size > MAX_CALLBACK_BYTES) {
        request.destroy();
        settleError(new ModuleDoctorError('BROWSER_CALLBACK_TOO_LARGE', 'browser callback exceeded the size limit'));
        return;
      }
      chunks.push(chunk);
    });
    request.on('end', () => {
      try {
        const body = JSON.parse(Buffer.concat(chunks).toString('utf8'));
        if (body.nonce !== expectedNonce) {
          throw new Error('nonce mismatch');
        }
        response.writeHead(200, { 'content-length': '0' });
        response.end(() => {
          clearTimeout(timeout);
          settleResult(body);
        });
      } catch {
        response.writeHead(400).end();
        settleError(new ModuleDoctorError('BROWSER_CALLBACK_INVALID', 'browser callback was invalid'));
      }
    });
  });
  await new Promise((resolveListen, rejectListen) => {
    server.once('error', rejectListen);
    server.listen(0, '127.0.0.1', resolveListen);
  });
  const address = server.address();
  const port = address && typeof address === 'object' ? address.port : 0;
  return {
    result,
    bindNonce(nonce) {
      expectedNonce = nonce;
    },
    origin() {
      return `http://127.0.0.1:${port}`;
    },
    url(nonce) {
      return `${this.origin()}/mango-module-doctor/${nonce}`;
    },
    close() {
      clearTimeout(timeout);
      return new Promise((resolveClose) => server.close(() => resolveClose()));
    },
  };
}

function validateBrowserResult(result, moduleCode) {
  if (
    !isRecord(result) ||
    result.schemaVersion !== 1 ||
    typeof result.nonce !== 'string' ||
    !/^[A-Za-z0-9_-]{22,86}$/.test(result.nonce) ||
    result.moduleCode !== moduleCode ||
    !isIsoInstant(result.observedAt) ||
    !isRecord(result.condition) ||
    result.condition.id !== CONDITION_ID ||
    !['PASS', 'FAIL', 'UNKNOWN'].includes(result.condition.status) ||
    result.condition.required !== true ||
    !isReasonCode(result.condition.reasonCode) ||
    !isIsoInstant(result.condition.observedAt) ||
    !isBoundedDuration(result.condition.durationMs, MAX_BROWSER_DURATION_MS) ||
    result.condition.stale !== false ||
    !validateBrowserEvidence(result.condition.evidence, result.condition.status) ||
    !validateBrowserVersion(result.frontendVersion)
  ) {
    throw new ModuleDoctorError('BROWSER_SCHEMA_UNSUPPORTED', 'browser diagnostic result schema is unsupported');
  }
}

function validateBrowserVersion(version) {
  if (
    !isRecord(version) ||
    version.source !== 'PAGE_REGISTRAR' ||
    !['PASS', 'UNKNOWN'].includes(version.status) ||
    !['FRONTEND_VERSION_OBSERVED', 'FRONTEND_VERSION_UNKNOWN'].includes(version.reasonCode)
  ) {
    return false;
  }
  return version.status === 'PASS'
    ? version.reasonCode === 'FRONTEND_VERSION_OBSERVED' && isBoundedText(version.value, 100)
    : version.reasonCode === 'FRONTEND_VERSION_UNKNOWN' && version.value === null;
}

function validateBrowserEvidence(evidence, conditionStatus) {
  if (
    !isRecord(evidence) ||
    !Number.isSafeInteger(evidence.expectedPageCount) ||
    evidence.expectedPageCount < 1 ||
    evidence.expectedPageCount > 32 ||
    !Number.isSafeInteger(evidence.failedPageCount) ||
    evidence.failedPageCount < 0 ||
    evidence.failedPageCount > evidence.expectedPageCount ||
    !Array.isArray(evidence.pages) ||
    evidence.pages.length > evidence.expectedPageCount ||
    !evidence.pages.every(validateBrowserPage)
  ) {
    return false;
  }
  const components = evidence.pages.map((page) => page.component);
  const actualFailed = evidence.pages.filter((page) => page.status === 'FAIL').length;
  if (new Set(components).size !== components.length || actualFailed !== evidence.failedPageCount) {
    return false;
  }
  if (conditionStatus === 'UNKNOWN') {
    return true;
  }
  if (evidence.pages.length !== evidence.expectedPageCount) {
    return false;
  }
  return conditionStatus === 'PASS' ? actualFailed === 0 : actualFailed > 0;
}

function validateBrowserPage(page) {
  return (
    isRecord(page) &&
    typeof page.component === 'string' &&
    SAFE_PAGE_REQUIREMENT.test(page.component) &&
    ['PASS', 'FAIL'].includes(page.status) &&
    isReasonCode(page.reasonCode)
  );
}

function unknownFrontendCondition(reasonCode, hint) {
  return {
    id: CONDITION_ID,
    status: 'UNKNOWN',
    required: true,
    reasonCode,
    evidence: hint ? { hint } : {},
    observedAt: new Date().toISOString(),
    durationMs: 0,
    stale: false,
  };
}

function unknownVersion(reasonCode) {
  return { value: null, source: 'PAGE_REGISTRAR', status: 'UNKNOWN', reasonCode };
}

function aggregateStatus(conditions) {
  const required = conditions.filter((condition) => condition.required);
  if (required.some((condition) => condition.status === 'FAIL')) {
    return 'FAILED';
  }
  if (required.some((condition) => condition.status === 'UNKNOWN' || condition.stale)) {
    return 'UNKNOWN';
  }
  if (required.some((condition) => condition.status === 'WARN')) {
    return 'DEGRADED';
  }
  const optional = conditions.filter((condition) => !condition.required);
  if (optional.some((condition) => condition.status === 'FAIL' || condition.status === 'WARN')) {
    return 'DEGRADED';
  }
  return 'READY';
}

function exitCodeForReport(report, strict) {
  if (report.status === 'FAILED') {
    return 1;
  }
  if (report.status === 'UNKNOWN' || report.status === 'DISABLED') {
    return 3;
  }
  if (report.conditions.some((condition) => condition.required && condition.status === 'WARN')) {
    return 1;
  }
  if (strict && (report.status === 'DEGRADED' || report.incompleteOptional)) {
    return 1;
  }
  return 0;
}

function writeResult(stdout, stderr, options, result) {
  if (options.json) {
    stdout.write(`${JSON.stringify(result)}\n`);
    return;
  }
  if (result.report) {
    stdout.write(`${result.moduleCode}: ${result.status}\n`);
    stdout.write(`  backend version: ${formatVersion(result.report.backendVersion)}\n`);
    stdout.write(`  frontend version: ${formatVersion(result.report.frontendVersion)}\n`);
    for (const condition of result.report.conditions) {
      stdout.write(`  ${condition.id}: ${condition.status} (${condition.reasonCode})\n`);
      if (typeof condition.evidence?.hint === 'string') {
        stdout.write(`    hint: ${condition.evidence.hint}\n`);
      }
    }
    return;
  }
  stderr.write(`Module diagnosis unavailable: ${result.error.reasonCode}: ${result.error.message}\n`);
}

function formatVersion(version) {
  return version?.value || `UNKNOWN (${version?.reasonCode || 'VERSION_UNKNOWN'})`;
}

function isLoopbackRemote(value) {
  return value === '127.0.0.1' || value === '::ffff:127.0.0.1' || value === '::1';
}

export function createIsolatedBrowserEnvironment(sourceEnvironment = process.env) {
  const allowed = [
    'HOME',
    'LANG',
    'LC_ALL',
    'PATH',
    'PLAYWRIGHT_BROWSERS_PATH',
    'SystemRoot',
    'TEMP',
    'TMP',
    'TMPDIR',
    'USERPROFILE',
    'WINDIR',
  ];
  return Object.fromEntries(
    allowed
      .map((name) => [name, sourceEnvironment[name]])
      .filter((entry) => typeof entry[1] === 'string' && entry[1].length > 0),
  );
}

function diagnosticHint(reasonCode) {
  return SAFE_DIAGNOSTIC_HINTS[reasonCode];
}

function isReasonCode(value) {
  return typeof value === 'string' && SAFE_REASON_CODE.test(value);
}

function isIsoInstant(value) {
  return (
    typeof value === 'string' &&
    value.length <= 40 &&
    /^\d{4}-\d{2}-\d{2}T/.test(value) &&
    Number.isFinite(Date.parse(value))
  );
}

function isBoundedDuration(value, maximum) {
  return Number.isSafeInteger(value) && value >= 0 && value <= maximum;
}

function isBoundedText(value, maxLength = MAX_TEXT_LENGTH) {
  return typeof value === 'string' && value.length > 0 && value.length <= maxLength;
}

function isBoundedJson(value, depth) {
  if (depth > 4 || value === undefined) {
    return false;
  }
  if (value === null || typeof value === 'boolean') {
    return true;
  }
  if (typeof value === 'string') {
    return value.length <= MAX_TEXT_LENGTH;
  }
  if (typeof value === 'number') {
    return Number.isFinite(value) && Math.abs(value) <= Number.MAX_SAFE_INTEGER;
  }
  if (Array.isArray(value)) {
    return value.length <= 64 && value.every((item) => isBoundedJson(item, depth + 1));
  }
  if (!isRecord(value)) {
    return false;
  }
  const entries = Object.entries(value);
  return (
    entries.length <= 32 &&
    entries.every(([key, item]) => /^[A-Za-z][A-Za-z0-9_.-]{0,79}$/.test(key) && isBoundedJson(item, depth + 1))
  );
}

function isRecord(value) {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}
