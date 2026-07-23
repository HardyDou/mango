import { probeRegisteredPage, type MangoPageProbeResult } from '@mango/admin-pages/core';

const FRAGMENT_PREFIX = '#mango-module-doctor=';
const CALLBACK_PATH_PREFIX = '/mango-module-doctor/';
const MAX_TTL_MS = 60_000;
const MAX_PAGE_REQUIREMENTS = 32;
const MAX_RESULT_BYTES = 64 * 1024;

export interface MangoModuleDiagnosticChallenge {
  schemaVersion: 1;
  nonce: string;
  expiresAt: number;
  frontendOrigin: string;
  callbackUrl: string;
  moduleCode: string;
  pageRequirements: string[];
}

export interface MangoModuleDiagnosticBrowserResult {
  schemaVersion: 1;
  nonce: string;
  moduleCode: string;
  observedAt: string;
  frontendVersion: {
    value: string | null;
    source: 'PAGE_REGISTRAR';
    status: 'PASS' | 'UNKNOWN';
    reasonCode: 'FRONTEND_VERSION_OBSERVED' | 'FRONTEND_VERSION_UNKNOWN';
  };
  condition: {
    id: 'frontend.pageRuntime';
    status: 'PASS' | 'FAIL' | 'UNKNOWN';
    required: true;
    reasonCode: string;
    evidence: {
      expectedPageCount: number;
      failedPageCount: number;
      pages: Array<Pick<MangoPageProbeResult, 'component' | 'status' | 'reasonCode'>>;
    };
    observedAt: string;
    durationMs: number;
    stale: false;
  };
}

export function hasModuleDiagnosticChallenge(
  browserWindow: Window | undefined = typeof window === 'undefined' ? undefined : window,
): boolean {
  if (!browserWindow) {
    return false;
  }
  const diagnosticWindow = browserWindow as Window & {
    __MANGO_PENDING_MODULE_DIAGNOSTIC_FRAGMENT__?: string;
  };
  return (diagnosticWindow.__MANGO_PENDING_MODULE_DIAGNOSTIC_FRAGMENT__ || browserWindow.location.hash).startsWith(
    FRAGMENT_PREFIX,
  );
}

export function parseModuleDiagnosticChallenge(
  fragment: string,
  currentOrigin: string,
  now = Date.now(),
): MangoModuleDiagnosticChallenge | undefined {
  return parseChallenge(fragment, currentOrigin, now).challenge;
}

function parseChallenge(
  fragment: string,
  currentOrigin: string,
  now: number,
): { challenge?: MangoModuleDiagnosticChallenge; reasonCode?: string } {
  if (!fragment.startsWith(FRAGMENT_PREFIX)) {
    return { reasonCode: 'FRAGMENT_PREFIX_INVALID' };
  }
  const encoded = fragment.slice(FRAGMENT_PREFIX.length);
  if (!encoded || encoded.length > 16_384 || encoded.includes('&')) {
    return { reasonCode: 'FRAGMENT_ENCODING_INVALID' };
  }
  let value: unknown;
  try {
    value = JSON.parse(decodeBase64Url(encoded));
  } catch {
    return { reasonCode: 'CHALLENGE_JSON_INVALID' };
  }
  if (!isRecord(value) || value.schemaVersion !== 1) {
    return { reasonCode: 'CHALLENGE_SCHEMA_INVALID' };
  }
  const challenge = value as unknown as MangoModuleDiagnosticChallenge;
  if (!/^[A-Za-z0-9_-]{22,86}$/.test(challenge.nonce)) {
    return { reasonCode: 'NONCE_INVALID' };
  }
  if (
    !Number.isSafeInteger(challenge.expiresAt) ||
    challenge.expiresAt < now ||
    challenge.expiresAt > now + MAX_TTL_MS
  ) {
    return { reasonCode: 'CHALLENGE_EXPIRED' };
  }
  if (challenge.frontendOrigin !== currentOrigin || !isLoopbackOrigin(currentOrigin)) {
    return { reasonCode: 'FRONTEND_ORIGIN_INVALID' };
  }
  if (!isSafeModuleCode(challenge.moduleCode)) {
    return { reasonCode: 'MODULE_CODE_INVALID' };
  }
  if (
    !Array.isArray(challenge.pageRequirements) ||
    challenge.pageRequirements.length === 0 ||
    challenge.pageRequirements.length > MAX_PAGE_REQUIREMENTS ||
    !challenge.pageRequirements.every(isSafePageRequirement) ||
    new Set(challenge.pageRequirements).size !== challenge.pageRequirements.length
  ) {
    return { reasonCode: 'PAGE_REQUIREMENTS_INVALID' };
  }
  if (!isValidCallback(challenge.callbackUrl, challenge.nonce)) {
    return { reasonCode: 'CALLBACK_INVALID' };
  }
  return { challenge: { ...challenge, pageRequirements: [...challenge.pageRequirements] } };
}

export async function runModuleDiagnosticBridge(
  registrarsReady: Promise<void>,
  dependencies: {
    window?: Window;
    probe?: typeof probeRegisteredPage;
    fetch?: typeof fetch;
    now?: () => number;
  } = {},
): Promise<MangoModuleDiagnosticBrowserResult | undefined> {
  const browserWindow = dependencies.window || (typeof window === 'undefined' ? undefined : window);
  if (!browserWindow) {
    return undefined;
  }
  const setState = (state: string) => {
    (browserWindow as Window & { __MANGO_MODULE_DIAGNOSTIC_STATE__?: string }).__MANGO_MODULE_DIAGNOSTIC_STATE__ =
      state;
  };
  const diagnosticWindow = browserWindow as Window & {
    __MANGO_PENDING_MODULE_DIAGNOSTIC_FRAGMENT__?: string;
  };
  const fragment = diagnosticWindow.__MANGO_PENDING_MODULE_DIAGNOSTIC_FRAGMENT__ || browserWindow.location.hash;
  delete diagnosticWindow.__MANGO_PENDING_MODULE_DIAGNOSTIC_FRAGMENT__;
  const now = dependencies.now || Date.now;
  const parsed = parseChallenge(fragment, browserWindow.location.origin, now());
  const challenge = parsed.challenge;
  if (!challenge) {
    setState(`CHALLENGE_REJECTED:${parsed.reasonCode}`);
    return undefined;
  }
  setState('CHALLENGE_ACCEPTED');
  browserWindow.history.replaceState(null, '', `${browserWindow.location.pathname}${browserWindow.location.search}`);

  const startedAt = now();
  let pages: MangoPageProbeResult[] = [];
  let bridgeFailure: string | undefined;
  try {
    await registrarsReady;
    pages = await Promise.all(
      challenge.pageRequirements.map((component) =>
        (dependencies.probe || probeRegisteredPage)(challenge.moduleCode, component),
      ),
    );
  } catch {
    bridgeFailure = 'FEATURE_REGISTRATION_FAILED';
  }
  setState(bridgeFailure || 'PAGES_PROBED');
  const observedAt = new Date(now()).toISOString();
  const failedPageCount = pages.filter((page) => page.status === 'FAIL').length;
  const versions = [...new Set(pages.map((page) => page.actualVersion).filter(isText))];
  const result: MangoModuleDiagnosticBrowserResult = {
    schemaVersion: 1,
    nonce: challenge.nonce,
    moduleCode: challenge.moduleCode,
    observedAt,
    frontendVersion: {
      value: versions.length === 1 ? versions[0]! : null,
      source: 'PAGE_REGISTRAR',
      status: versions.length === 1 ? 'PASS' : 'UNKNOWN',
      reasonCode: versions.length === 1 ? 'FRONTEND_VERSION_OBSERVED' : 'FRONTEND_VERSION_UNKNOWN',
    },
    condition: {
      id: 'frontend.pageRuntime',
      status: bridgeFailure ? 'UNKNOWN' : failedPageCount > 0 ? 'FAIL' : 'PASS',
      required: true,
      reasonCode: bridgeFailure || (failedPageCount > 0 ? 'PAGE_RUNTIME_FAILED' : 'PAGE_RUNTIME_READY'),
      evidence: {
        expectedPageCount: challenge.pageRequirements.length,
        failedPageCount,
        pages: pages.map(({ component, status, reasonCode }) => ({ component, status, reasonCode })),
      },
      observedAt,
      durationMs: Math.max(0, now() - startedAt),
      stale: false,
    },
  };
  const body = JSON.stringify(result);
  if (new TextEncoder().encode(body).byteLength > MAX_RESULT_BYTES) {
    return undefined;
  }
  setState('CALLBACK_POSTING');
  try {
    const response = await (dependencies.fetch || fetch)(challenge.callbackUrl, {
      method: 'POST',
      headers: { 'content-type': 'text/plain;charset=UTF-8' },
      body,
      credentials: 'omit',
      cache: 'no-store',
      redirect: 'error',
      referrerPolicy: 'no-referrer',
    });
    if (!response.ok) {
      throw new Error(`module diagnostic callback rejected with status ${response.status}`);
    }
  } catch (error) {
    setState('CALLBACK_FAILED');
    throw error;
  }
  setState('COMPLETE');
  return result;
}

function decodeBase64Url(value: string): string {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
  return decodeURIComponent(
    Array.from(atob(padded), (character) => `%${character.charCodeAt(0).toString(16).padStart(2, '0')}`).join(''),
  );
}

function isValidCallback(value: unknown, nonce: string): value is string {
  if (!isText(value)) {
    return false;
  }
  try {
    const url = new URL(value);
    return (
      url.protocol === 'http:' &&
      isLoopbackHost(url.hostname) &&
      !url.username &&
      !url.password &&
      !url.search &&
      !url.hash &&
      url.pathname === `${CALLBACK_PATH_PREFIX}${nonce}`
    );
  } catch {
    return false;
  }
}

function isLoopbackOrigin(value: string): boolean {
  try {
    const url = new URL(value);
    return (
      url.protocol === 'http:' && isLoopbackHost(url.hostname) && url.origin === value && !url.username && !url.password
    );
  } catch {
    return false;
  }
}

function isLoopbackHost(hostname: string): boolean {
  return hostname === '127.0.0.1' || hostname === '[::1]';
}

function isSafeModuleCode(value: unknown): value is string {
  return isText(value) && value.length <= 80 && /^[a-z0-9][a-z0-9-]*$/.test(value);
}

function isSafePageRequirement(value: unknown): value is string {
  return isText(value) && value.length <= 200 && /^[A-Za-z0-9_./-]+$/.test(value);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function isText(value: unknown): value is string {
  return typeof value === 'string' && value.length > 0;
}
