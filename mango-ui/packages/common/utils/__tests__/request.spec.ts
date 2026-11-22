import { beforeEach, describe, expect, it, vi } from 'vitest';
import { clearDataGrants } from '@mango/http-client';
import { Session } from '../storage';
import { resolveHttpErrorMessage } from '../request';
import { mangoMessage } from '../message';

const mocks = vi.hoisted(() => ({
  handlers: [] as any[],
  service: Object.assign(vi.fn(), {
    interceptors: {
      request: {
        use: vi.fn((fulfilled, rejected) => {
          mocks.handlers.push({ type: 'request', fulfilled, rejected });
        }),
      },
      response: {
        use: vi.fn((fulfilled, rejected) => {
          mocks.handlers.push({ type: 'response', fulfilled, rejected });
        }),
      },
    },
    get: vi.fn(),
    post: vi.fn(),
  }) as any,
  error: vi.fn(),
  warning: vi.fn(),
}));

vi.mock('axios', async (importOriginal) => {
  const actual = await importOriginal<typeof import('axios')>();
  return {
    ...actual,
    default: {
      ...actual.default,
      create: vi.fn(() => mocks.service),
    },
  };
});

vi.mock('../message', () => ({
  mangoMessage: {
    error: mocks.error,
    warning: mocks.warning,
  },
}));

describe('request utilities', () => {
  beforeEach(() => {
    sessionStorage.clear();
    document.cookie = 'MANGO_TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
    clearDataGrants();
    vi.clearAllMocks();
  });

  it('uses backend business message for non-success 200 responses', async () => {
    const interceptor = responseInterceptor();
    const response = {
      config: {},
      data: { code: 3651, success: false, msg: '不能转办给自己', data: null },
    } as any;

    await expect(interceptor.fulfilled(response)).rejects.toMatchObject({
      message: '不能转办给自己',
      code: 3651,
    });
    expect(mangoMessage.error).toHaveBeenCalledWith('不能转办给自己');
  });

  it('refreshes an expiring access token once and continues the original request', async () => {
    Session.setToken('old-token', {
      refreshToken: 'refresh-token',
      expiresAt: Date.now() + 10_000,
    });
    vi.mocked(mocks.service.post).mockResolvedValueOnce({
      accessToken: 'new-token',
      refreshToken: 'new-refresh-token',
      expiresIn: 7200,
      tenantId: '1',
    });

    const config = await requestInterceptor().fulfilled({ url: '/workflow/tasks/todo', headers: {} });

    expect(mocks.service.post).toHaveBeenCalledWith(
      '/auth/refresh',
      { refreshToken: 'refresh-token' },
      expect.objectContaining({
        ignoreToken: true,
        skipRefreshToken: true,
        silentError: true,
      }),
    );
    expect(Session.getToken()).toBe('new-token');
    expect(Session.getRefreshToken()).toBe('new-refresh-token');
    expect(config.headers.Authorization).toBe('Bearer new-token');
  });

  it('refreshes and retries when backend returns auth business code', async () => {
    Session.setToken('old-token', {
      refreshToken: 'refresh-token',
      expiresAt: Date.now() + 3_600_000,
    });
    vi.mocked(mocks.service.post).mockResolvedValueOnce({
      accessToken: 'new-token',
      refreshToken: 'new-refresh-token',
      expiresIn: 7200,
    });
    vi.mocked(mocks.service as any).mockResolvedValueOnce({ ok: true });

    const response = {
      config: { url: '/auth/userinfo', headers: {} },
      data: { code: 1411, success: false, msg: '未登录或登录已过期', data: null },
    } as any;

    await expect(responseInterceptor().fulfilled(response)).resolves.toEqual({ ok: true });
    expect(mocks.service.post).toHaveBeenCalledWith(
      '/auth/refresh',
      { refreshToken: 'refresh-token' },
      expect.objectContaining({
        ignoreToken: true,
        skipRefreshToken: true,
        silentError: true,
      }),
    );
    expect(mocks.service).toHaveBeenCalledWith(
      expect.objectContaining({
        url: '/auth/userinfo',
        _retry: true,
        headers: expect.objectContaining({ Authorization: 'Bearer new-token' }),
      }),
    );
  });

  it('resolves HTTP errors from backend message before fallback status text', () => {
    expect(resolveHttpErrorMessage(500, { msg: '数据库操作异常' }, 'Request failed')).toBe('数据库操作异常');
    expect(resolveHttpErrorMessage(500, undefined, undefined)).toBe('服务器内部错误');
  });

  it('forwards the data grant issued by a list response to the following detail request', async () => {
    loginAs('10023', '1');
    const request = requestInterceptor();
    const response = responseInterceptor();

    const listConfig = { url: '/fixture/items/page', headers: {} } as any;
    await request.fulfilled(listConfig);
    await response.fulfilled({
      config: listConfig,
      headers: { 'x-mango-data-grant': 'grant-a', 'x-mango-data-grant-domain': 'fixture:item' },
      data: { code: 200, success: true, data: [] },
    });

    const detailConfig = { url: '/fixture/items/detail', headers: {} } as any;
    await request.fulfilled(detailConfig);

    expect(new detailConfig.headers.constructor(detailConfig.headers).get('X-Mango-Data-Grant')).toBe('grant-a');
  });

  it('drops the data grant when a detail request answers with an absent marker', async () => {
    loginAs('10023', '1');
    const request = requestInterceptor();
    const response = responseInterceptor();

    const listConfig = { url: '/fixture/items/page', headers: {} } as any;
    await request.fulfilled(listConfig);
    await response.fulfilled({
      config: listConfig,
      headers: { 'x-mango-data-grant': 'grant-a', 'x-mango-data-grant-domain': 'fixture:item' },
      data: { code: 200, success: true, data: [] },
    });

    const detailConfig = { url: '/fixture/items/detail', headers: {} } as any;
    await request.fulfilled(detailConfig);
    await expect(
      response.rejected({
        config: detailConfig,
        response: {
          status: 403,
          headers: { 'x-mango-data-grant': 'absent', 'x-mango-data-grant-domain': 'fixture:item' },
          data: { code: 403, message: '缺少数据权限凭证，请刷新列表后重试' },
        },
        message: 'Request failed with status code 403',
      }),
    ).rejects.toBeDefined();

    const nextConfig = { url: '/fixture/items/detail', headers: {} } as any;
    await request.fulfilled(nextConfig);

    expect(new nextConfig.headers.constructor(nextConfig.headers).get('X-Mango-Data-Grant')).toBeUndefined();
  });

  it('does not attach the data grant to stream downloads or auth-less requests', async () => {
    loginAs('10023', '1');
    const request = requestInterceptor();
    const response = responseInterceptor();

    const listConfig = { url: '/fixture/items/page', headers: {} } as any;
    await request.fulfilled(listConfig);
    await response.fulfilled({
      config: listConfig,
      headers: { 'x-mango-data-grant': 'grant-a', 'x-mango-data-grant-domain': 'fixture:item' },
      data: { code: 200, success: true, data: [] },
    });

    const streamConfig = { url: '/files/1/download', headers: {}, responseType: 'stream' } as any;
    await request.fulfilled(streamConfig);
    expect(new streamConfig.headers.constructor(streamConfig.headers).get('X-Mango-Data-Grant')).toBeUndefined();

    const publicConfig = { url: '/auth/refresh', headers: {}, ignoreToken: true } as any;
    await request.fulfilled(publicConfig);
    expect(new publicConfig.headers.constructor(publicConfig.headers).get('X-Mango-Data-Grant')).toBeUndefined();
  });

  it('discards a cached data grant when the identity changes', async () => {
    loginAs('10023', '1');
    const request = requestInterceptor();
    const response = responseInterceptor();

    const listConfig = { url: '/fixture/items/page', headers: {} } as any;
    await request.fulfilled(listConfig);
    await response.fulfilled({
      config: listConfig,
      headers: { 'x-mango-data-grant': 'grant-a', 'x-mango-data-grant-domain': 'fixture:item' },
      data: { code: 200, success: true, data: [] },
    });

    loginAs('10024', '1');
    const otherUserConfig = { url: '/fixture/items/detail', headers: {} } as any;
    await request.fulfilled(otherUserConfig);
    expect(new otherUserConfig.headers.constructor(otherUserConfig.headers).get('X-Mango-Data-Grant')).toBeUndefined();

    loginAs('10023', '1');
    const backAgainConfig = { url: '/fixture/items/detail', headers: {} } as any;
    await request.fulfilled(backAgainConfig);
    expect(new backAgainConfig.headers.constructor(backAgainConfig.headers).get('X-Mango-Data-Grant')).toBeUndefined();
  });

  it('clears cached data grants together with the token on logout', async () => {
    loginAs('10023', '1');
    const request = requestInterceptor();
    const response = responseInterceptor();

    const listConfig = { url: '/fixture/items/page', headers: {} } as any;
    await request.fulfilled(listConfig);
    await response.fulfilled({
      config: listConfig,
      headers: { 'x-mango-data-grant': 'grant-a', 'x-mango-data-grant-domain': 'fixture:item' },
      data: { code: 200, success: true, data: [] },
    });

    Session.clearToken();

    const detailConfig = { url: '/fixture/items/detail', headers: {} } as any;
    await request.fulfilled(detailConfig);
    expect(new detailConfig.headers.constructor(detailConfig.headers).get('X-Mango-Data-Grant')).toBeUndefined();
  });
});

function loginAs(userId: string, tenantId: string): void {
  Session.set('userInfo', { userId, tenantId });
}

function requestInterceptor() {
  return mocks.handlers.find((item) => item.type === 'request');
}

function responseInterceptor() {
  return mocks.handlers.find((item) => item.type === 'response');
}
