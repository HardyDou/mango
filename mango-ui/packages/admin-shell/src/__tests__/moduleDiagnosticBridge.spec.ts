import { describe, expect, it, vi } from 'vitest';

import {
  parseModuleDiagnosticChallenge,
  runModuleDiagnosticBridge,
  type MangoModuleDiagnosticChallenge,
} from '../runtime/moduleDiagnosticBridge';

const NOW = 1_800_000_000_000;
const NONCE = 'abcdefghijklmnopqrstuv';

function challenge(overrides: Partial<MangoModuleDiagnosticChallenge> = {}) {
  return {
    schemaVersion: 1 as const,
    nonce: NONCE,
    expiresAt: NOW + 30_000,
    frontendOrigin: 'http://127.0.0.1:5176',
    callbackUrl: `http://127.0.0.1:43123/mango-module-doctor/${NONCE}`,
    moduleCode: 'mango-link',
    pageRequirements: ['link/items/index'],
    ...overrides,
  };
}

function fragment(value: MangoModuleDiagnosticChallenge) {
  const bytes = new TextEncoder().encode(JSON.stringify(value));
  let binary = '';
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return `#mango-module-doctor=${btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')}`;
}

describe('module diagnostic bridge', () => {
  it('accepts an exact, unexpired loopback challenge', () => {
    expect(parseModuleDiagnosticChallenge(fragment(challenge()), 'http://127.0.0.1:5176', NOW)).toEqual(challenge());
  });

  it('rejects remote, expired, mismatched-origin and malformed callback challenges', () => {
    expect(
      parseModuleDiagnosticChallenge(
        fragment(challenge({ frontendOrigin: 'https://example.com' })),
        'https://example.com',
        NOW,
      ),
    ).toBeUndefined();
    expect(
      parseModuleDiagnosticChallenge(fragment(challenge({ expiresAt: NOW - 1 })), 'http://127.0.0.1:5176', NOW),
    ).toBeUndefined();
    expect(parseModuleDiagnosticChallenge(fragment(challenge()), 'http://127.0.0.1:5177', NOW)).toBeUndefined();
    expect(
      parseModuleDiagnosticChallenge(
        fragment(challenge({ callbackUrl: `http://localhost:43123/mango-module-doctor/${NONCE}` })),
        'http://127.0.0.1:5176',
        NOW,
      ),
    ).toBeUndefined();
  });

  it('clears the fragment, probes only requested pages and posts a bounded credential-free result', async () => {
    const replaceState = vi.fn();
    const post = vi.fn().mockResolvedValue({ ok: true });
    const probe = vi.fn().mockResolvedValue({
      moduleCode: 'mango-link',
      component: 'link/items/index',
      packageName: '@mango/link',
      actualVersion: '1.0.14',
      status: 'PASS',
      reasonCode: 'PAGE_RUNTIME_READY',
      stages: {},
    });
    const browserWindow = {
      location: {
        hash: fragment(challenge()),
        origin: 'http://127.0.0.1:5176',
        pathname: '/',
        search: '',
      },
      history: { replaceState },
    } as unknown as Window;

    const result = await runModuleDiagnosticBridge(Promise.resolve(), {
      window: browserWindow,
      probe,
      fetch: post,
      now: () => NOW,
    });

    expect(replaceState).toHaveBeenCalledWith(null, '', '/');
    expect(probe).toHaveBeenCalledTimes(1);
    expect(probe).toHaveBeenCalledWith('mango-link', 'link/items/index');
    expect(result?.condition.status).toBe('PASS');
    expect(result?.frontendVersion.value).toBe('1.0.14');
    expect(post).toHaveBeenCalledWith(
      challenge().callbackUrl,
      expect.objectContaining({
        method: 'POST',
        headers: { 'content-type': 'text/plain;charset=UTF-8' },
        credentials: 'omit',
        redirect: 'error',
        referrerPolicy: 'no-referrer',
      }),
    );
  });

  it('fails closed when the loopback callback rejects the result', async () => {
    const browserWindow = {
      location: {
        hash: fragment(challenge()),
        origin: 'http://127.0.0.1:5176',
        pathname: '/',
        search: '',
      },
      history: { replaceState: vi.fn() },
    } as unknown as Window;

    await expect(
      runModuleDiagnosticBridge(Promise.resolve(), {
        window: browserWindow,
        probe: vi.fn().mockResolvedValue({
          moduleCode: 'mango-link',
          component: 'link/items/index',
          status: 'PASS',
          reasonCode: 'PAGE_RUNTIME_READY',
          stages: {},
        }),
        fetch: vi.fn().mockResolvedValue({ ok: false, status: 403 }),
        now: () => NOW,
      }),
    ).rejects.toThrow('callback rejected');
    expect(
      (browserWindow as Window & { __MANGO_MODULE_DIAGNOSTIC_STATE__?: string }).__MANGO_MODULE_DIAGNOSTIC_STATE__,
    ).toBe('CALLBACK_FAILED');
  });
});
