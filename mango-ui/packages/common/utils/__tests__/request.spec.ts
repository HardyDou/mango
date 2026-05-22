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

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => mocks.service),
  },
}));

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

    expect(mocks.service.post).toHaveBeenCalledWith('/auth/refresh', { refreshToken: 'refresh-token' }, expect.objectContaining({
      ignoreToken: true,
      skipRefreshToken: true,
      silentError: true,
    }));
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
    expect(mocks.service.post).toHaveBeenCalledWith('/auth/refresh', { refreshToken: 'refresh-token' }, expect.objectContaining({
      ignoreToken: true,
      skipRefreshToken: true,
      silentError: true,
    }));
    expect(mocks.service).toHaveBeenCalledWith(expect.objectContaining({
      url: '/auth/userinfo',
      _retry: true,
      headers: expect.objectContaining({ Authorization: 'Bearer new-token' }),
    }));
  });

  it('resolves HTTP errors from backend message before fallback status text', () => {
    expect(resolveHttpErrorMessage(500, { msg: '数据库操作异常' }, 'Request failed')).toBe('数据库操作异常');
    expect(resolveHttpErrorMessage(500, undefined, undefined)).toBe('服务器内部错误');
  });
});

function requestInterceptor() {
  return mocks.handlers.find(item => item.type === 'request');
}

function responseInterceptor() {
  return mocks.handlers.find(item => item.type === 'response');
}
