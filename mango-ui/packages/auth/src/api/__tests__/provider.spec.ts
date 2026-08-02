import { beforeEach, describe, expect, it, vi } from 'vitest';

const request = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}));

vi.mock('@mango/common/utils/request', () => request);

import {
  bindExistingProviderAccount,
  completeProviderAuthorization,
  listAvailableProviders,
  providerCallbackUri,
  saveProviderConfig,
  startProviderAuthorization,
} from '../provider';

describe('provider authorization API', () => {
  beforeEach(() => vi.clearAllMocks());

  it('discovers providers by tenant and app without a login token', async () => {
    await listAvailableProviders('1', 'internal-admin');

    expect(request.get).toHaveBeenCalledWith('/auth/providers', {
      params: { tenantId: '1', appCode: 'internal-admin' },
      ignoreToken: true,
    });
  });

  it('keeps login authorization public and current-user binding authenticated', async () => {
    const common = {
      tenantId: '1',
      appCode: 'internal-admin',
      provider: 'WECOM' as const,
      redirectUri: 'https://admin.example.com/provider-callback',
    };
    await startProviderAuthorization({ ...common, intent: 'LOGIN' });
    await startProviderAuthorization({ ...common, intent: 'BIND_CURRENT' });

    expect(request.post).toHaveBeenNthCalledWith(
      1,
      '/auth/providers/authorize',
      { ...common, intent: 'LOGIN' },
      { ignoreToken: true },
    );
    expect(request.post).toHaveBeenNthCalledWith(
      2,
      '/auth/providers/authorize',
      { ...common, intent: 'BIND_CURRENT' },
      { ignoreToken: false },
    );
  });

  it('uses public one-time callback and bind-existing endpoints', async () => {
    await completeProviderAuthorization({ state: 'state', code: 'code' });
    await bindExistingProviderAccount({ bindingTicket: 'ticket', username: 'user', password: 'password' });

    expect(request.post).toHaveBeenNthCalledWith(
      1,
      '/auth/providers/complete',
      { state: 'state', code: 'code' },
      { ignoreToken: true },
    );
    expect(request.post).toHaveBeenNthCalledWith(
      2,
      '/auth/providers/bind-existing',
      { bindingTicket: 'ticket', username: 'user', password: 'password' },
      { ignoreToken: true },
    );
  });

  it('creates or updates provider configs without changing secret semantics', async () => {
    const config = {
      appCode: 'internal-admin',
      provider: 'DINGTALK' as const,
      clientId: 'client',
      redirectUris: ['https://admin.example.com/provider-callback'],
      enabled: true,
    };
    await saveProviderConfig(config);
    await saveProviderConfig({ ...config, id: 8 });

    expect(request.post).toHaveBeenCalledWith('/auth/provider-configs', config);
    expect(request.put).toHaveBeenCalledWith('/auth/provider-configs', {
      ...config,
      id: 8,
    });
  });

  it('builds an OAuth callback URI without a fragment or provider state', () => {
    window.history.replaceState(null, '', '/admin/');
    expect(providerCallbackUri()).toBe(`${window.location.origin}/admin/`);
  });
});
