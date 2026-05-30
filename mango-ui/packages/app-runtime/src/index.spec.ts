import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  MangoRuntimeConfigError,
  isValidRuntimeEntry,
  loadRuntimeConfigWithOptions,
  normalizeRuntimeConfig,
} from './index';

describe('runtime config validation', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('requires an allowlist when configured for production remote entries', () => {
    expect(isValidRuntimeEntry('https://unknown.mango.io/', {
      allowHttpEntries: false,
      requireEntryAllowlist: true,
      allowedEntryOrigins: ['https://rbac.mango.io'],
    })).toBe(false);

    expect(isValidRuntimeEntry('https://rbac.mango.io/', {
      allowHttpEntries: false,
      requireEntryAllowlist: true,
      allowedEntryOrigins: ['https://rbac.mango.io'],
    })).toBe(true);
  });

  it('does not allow production host wildcard style matching when only exact origins are configured', () => {
    const productionOptions = {
      allowHttpEntries: false,
      requireEntryAllowlist: true,
      allowedEntryOrigins: ['https://rbac.mango.io'],
      allowedEntryHosts: [],
    };

    expect(isValidRuntimeEntry('https://workflow.mango.io/', productionOptions)).toBe(false);
    expect(isValidRuntimeEntry('https://rbac.mango.io/', productionOptions)).toBe(true);
  });

  it('rejects http entries unless development explicitly allows them', () => {
    expect(isValidRuntimeEntry('http://b.mango.io:5181/', {
      allowHttpEntries: false,
      allowedEntryOrigins: ['http://b.mango.io:5181'],
    })).toBe(false);

    expect(isValidRuntimeEntry('http://b.mango.io:5181/', {
      allowHttpEntries: true,
      allowedEntryOrigins: ['http://b.mango.io:5181'],
    })).toBe(true);
  });

  it('reports invalid micro entries as error diagnostics', () => {
    const config = normalizeRuntimeConfig({
      profile: 'hybrid',
      modules: {
        'mango-authorization': {
          mode: 'micro',
          runtimeCode: 'mango-admin-rbac-app',
          entry: 'https://unknown.mango.io/',
        },
      },
    }, {
      allowHttpEntries: false,
      requireEntryAllowlist: true,
      allowedEntryOrigins: ['https://rbac.mango.io'],
    });

    expect(config.diagnostics).toContainEqual(expect.objectContaining({
      level: 'error',
      moduleCode: 'mango-authorization',
      field: 'entry',
    }));
  });

  it('exposes a typed runtime config error for fail-closed callers', () => {
    const error = new MangoRuntimeConfigError('Runtime config validation failed', [
      {
        level: 'error',
        moduleCode: 'mango-authorization',
        field: 'entry',
        message: 'invalid entry',
      },
    ]);

    expect(error.name).toBe('MangoRuntimeConfigError');
    expect(error.diagnostics?.[0].field).toBe('entry');
  });

  it('falls back to defaults when non fail-closed runtime config contains invalid JSON', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response('<!doctype html>', {
      status: 200,
      headers: { 'content-type': 'text/html' },
    })));

    const config = await loadRuntimeConfigWithOptions({
      profile: 'monolith',
      modules: {
        'mango-system': {
          mode: 'local',
          runtimeCode: 'mango-admin-system-local',
        },
      },
    }, {
      configUrl: '/runtime-config.json',
      failClosed: false,
    });

    expect(config.profile).toBe('monolith');
    expect(config.modules['mango-system'].mode).toBe('local');
  });

  it('throws when fail-closed runtime config contains invalid JSON', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response('<!doctype html>', {
      status: 200,
      headers: { 'content-type': 'text/html' },
    })));

    await expect(loadRuntimeConfigWithOptions({
      profile: 'monolith',
      modules: {},
    }, {
      configUrl: '/runtime-config.json',
      failClosed: true,
    })).rejects.toThrow(MangoRuntimeConfigError);
  });
});
