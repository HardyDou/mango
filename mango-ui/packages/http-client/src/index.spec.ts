import { beforeEach, describe, expect, it, vi } from 'vitest';
import { clearDataGrants } from './dataGrant';
import { createMangoHttpClient, MangoHttpError } from './index';

const mocks = vi.hoisted(() => {
  const instances: Array<{
    request: ReturnType<typeof vi.fn>;
    interceptors: {
      request: { use: ReturnType<typeof vi.fn>; eject: ReturnType<typeof vi.fn> };
      response: { use: ReturnType<typeof vi.fn>; eject: ReturnType<typeof vi.fn> };
    };
  }> = [];
  let interceptorId = 0;
  const create = vi.fn(() => {
    const instance = {
      request: vi.fn(),
      interceptors: {
        request: {
          use: vi.fn(() => ++interceptorId),
          eject: vi.fn(),
        },
        response: {
          use: vi.fn(() => ++interceptorId),
          eject: vi.fn(),
        },
      },
    };
    instances.push(instance);
    return instance;
  });
  return {
    create,
    instances,
    isAxiosError: vi.fn((value: unknown) => Boolean((value as { isAxiosError?: boolean })?.isAxiosError)),
    reset() {
      instances.splice(0);
      interceptorId = 0;
      create.mockClear();
    },
  };
});

vi.mock('axios', () => ({
  default: {
    create: mocks.create,
    isAxiosError: mocks.isAxiosError,
  },
}));

describe('createMangoHttpClient', () => {
  beforeEach(() => {
    mocks.reset();
    mocks.isAxiosError.mockClear();
    clearDataGrants();
  });

  it('isolates base URL, token, tenant and trace context between instances', async () => {
    const first = createMangoHttpClient({
      baseUrl: '/tenant-a-api',
      getAccessToken: () => 'token-a',
      getTenantId: () => 'tenant-a',
      getTraceId: () => 'trace-a',
    });
    const second = createMangoHttpClient({
      baseUrl: '/tenant-b-api',
      getAccessToken: () => 'token-b',
      getTenantId: () => 'tenant-b',
      getTraceId: () => 'trace-b',
    });
    mocks.instances[0].request.mockResolvedValue(successResponse({ owner: 'a' }));
    mocks.instances[1].request.mockResolvedValue(successResponse({ owner: 'b' }));

    await expect(first.request({ method: 'GET', url: '/orders' })).resolves.toEqual({ owner: 'a' });
    await expect(second.request({ method: 'GET', url: '/orders' })).resolves.toEqual({ owner: 'b' });

    expect(mocks.create).toHaveBeenNthCalledWith(1, expect.objectContaining({ baseURL: '/tenant-a-api' }));
    expect(mocks.create).toHaveBeenNthCalledWith(2, expect.objectContaining({ baseURL: '/tenant-b-api' }));
    expect(mocks.instances[0].request).toHaveBeenCalledWith(
      expect.objectContaining({
        headers: expect.objectContaining({
          Authorization: 'Bearer token-a',
          'TENANT-ID': 'tenant-a',
          'X-Mango-Tenant-Id': 'tenant-a',
          'X-Trace-Id': 'trace-a',
        }),
      }),
    );
    expect(mocks.instances[1].request).toHaveBeenCalledWith(
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer token-b', 'TENANT-ID': 'tenant-b' }),
      }),
    );
  });

  it('shares one refresh only inside the same client and retries both unauthorized requests', async () => {
    let finishRefresh: ((token: string) => void) | undefined;
    const refreshAccessToken = vi.fn(
      () =>
        new Promise<string>((resolve) => {
          finishRefresh = resolve;
        }),
    );
    const client = createMangoHttpClient({
      baseUrl: '/api',
      getAccessToken: () => 'expired-token',
      refreshAccessToken,
    });
    let dispatchCount = 0;
    mocks.instances[0].request.mockImplementation(() => {
      dispatchCount += 1;
      return dispatchCount <= 2
        ? Promise.reject(axiosFailure({ status: 401, message: 'expired' }))
        : Promise.resolve(successResponse({ dispatchCount }));
    });

    const first = client.request({ method: 'GET', url: '/orders/1' });
    const second = client.request({ method: 'GET', url: '/orders/2' });
    await vi.waitFor(() => expect(refreshAccessToken).toHaveBeenCalledTimes(1));
    finishRefresh?.('fresh-token');

    await expect(first).resolves.toEqual({ dispatchCount: 3 });
    await expect(second).resolves.toEqual({ dispatchCount: 4 });
    expect(refreshAccessToken).toHaveBeenCalledTimes(1);
    expect(mocks.instances[0].request).toHaveBeenNthCalledWith(
      3,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer fresh-token' }),
      }),
    );
    expect(mocks.instances[0].request).toHaveBeenNthCalledWith(
      4,
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer fresh-token' }),
      }),
    );
  });

  it('retries only safe or explicitly idempotent requests', async () => {
    const client = createMangoHttpClient({ baseUrl: '/api', maxRetries: 1 });
    const transport = mocks.instances[0].request;
    transport.mockRejectedValueOnce(axiosFailure({ code: 'ETIMEDOUT', message: 'timeout' }));

    await expect(client.request({ method: 'POST', url: '/orders', body: {} })).rejects.toMatchObject({
      kind: 'timeout',
    });
    expect(transport).toHaveBeenCalledTimes(1);

    transport.mockRejectedValueOnce(axiosFailure({ code: 'ETIMEDOUT', message: 'timeout' }));
    transport.mockResolvedValueOnce(successResponse({ id: '100' }));
    await expect(
      client.request({
        method: 'POST',
        url: '/orders',
        body: {},
        idempotencyKey: 'order-100',
      }),
    ).resolves.toEqual({ id: '100' });
    expect(transport).toHaveBeenCalledTimes(3);
    expect(transport).toHaveBeenLastCalledWith(
      expect.objectContaining({
        headers: expect.objectContaining({ 'Idempotency-Key': 'order-100' }),
      }),
    );
  });

  it('cancels pending work on deactivate and ejects interceptors on idempotent destroy', async () => {
    const client = createMangoHttpClient({ baseUrl: '/api' });
    const transport = mocks.instances[0];
    transport.request.mockImplementation(
      (config: { signal: AbortSignal }) =>
        new Promise((_resolve, reject) => {
          config.signal.addEventListener('abort', () => {
            reject(axiosFailure({ code: 'ERR_CANCELED', message: 'canceled' }));
          });
        }),
    );

    const pending = client.request({ method: 'GET', url: '/slow' });
    await vi.waitFor(() => expect(transport.request).toHaveBeenCalledTimes(1));
    client.deactivate();

    await expect(pending).rejects.toMatchObject({ name: 'HttpError', kind: 'aborted' });
    expect(client.state).toBe('inactive');
    await expect(client.request({ method: 'GET', url: '/blocked' })).rejects.toMatchObject({
      kind: 'configuration',
    });

    client.activate();
    transport.request.mockResolvedValueOnce(successResponse({ active: true }));
    await expect(client.request({ method: 'GET', url: '/active' })).resolves.toEqual({ active: true });

    client.destroy();
    client.destroy();
    expect(client.state).toBe('destroyed');
    expect(transport.interceptors.request.eject).toHaveBeenCalledTimes(1);
    expect(transport.interceptors.response.eject).toHaveBeenCalledTimes(1);
    expect(() => client.activate()).toThrow(MangoHttpError);
  });

  it('aborts an in-flight token refresh without invoking unauthorized handling', async () => {
    let refreshSignal: AbortSignal | undefined;
    const onUnauthorized = vi.fn();
    const refreshAccessToken = vi.fn(
      (signal: AbortSignal) =>
        new Promise<string>((_resolve, reject) => {
          refreshSignal = signal;
          signal.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')));
        }),
    );
    const client = createMangoHttpClient({
      baseUrl: '/api',
      getAccessToken: () => 'expired-token',
      refreshAccessToken,
      onUnauthorized,
    });
    mocks.instances[0].request.mockRejectedValue(axiosFailure({ status: 401, message: 'expired' }));

    const pending = client.request({ method: 'GET', url: '/orders' });
    await vi.waitFor(() => expect(refreshAccessToken).toHaveBeenCalledTimes(1));
    client.deactivate('micro app unmounted');

    await expect(pending).rejects.toMatchObject({ kind: 'aborted' });
    expect(refreshSignal?.aborted).toBe(true);
    expect(onUnauthorized).not.toHaveBeenCalled();
  });

  it('normalizes a synchronous refresh failure and invokes unauthorized handling', async () => {
    const onUnauthorized = vi.fn();
    const client = createMangoHttpClient({
      baseUrl: '/api',
      getAccessToken: () => 'expired-token',
      refreshAccessToken: () => {
        throw new Error('refresh provider failed synchronously');
      },
      onUnauthorized,
    });
    mocks.instances[0].request.mockRejectedValue(axiosFailure({ status: 401, message: 'expired' }));

    await expect(client.request({ method: 'GET', url: '/orders' })).rejects.toMatchObject({
      kind: 'unauthorized',
      status: 401,
    });
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });

  it('does not let a host unauthorized handler replace the normalized request failure', async () => {
    const client = createMangoHttpClient({
      baseUrl: '/api',
      getAccessToken: () => 'expired-token',
      onUnauthorized: () => Promise.reject(new Error('router unavailable')),
    });
    mocks.instances[0].request.mockRejectedValue(axiosFailure({ status: 401, message: 'expired' }));

    await expect(client.request({ method: 'GET', url: '/orders' })).rejects.toMatchObject({
      name: 'HttpError',
      kind: 'unauthorized',
      status: 401,
      message: 'expired',
    });
  });

  it('maps transport progress into the vendor-neutral public shape', async () => {
    const uploadProgress = vi.fn();
    const downloadProgress = vi.fn();
    const client = createMangoHttpClient({ baseUrl: '/api' });
    mocks.instances[0].request.mockImplementation((config) => {
      config.onUploadProgress?.({ loaded: 4, total: 10, progress: 0.4, bytes: 4, rate: 20 });
      config.onDownloadProgress?.({ loaded: 10, total: 10, progress: 1, bytes: 6, rate: 30 });
      return Promise.resolve(successResponse({ uploaded: true }));
    });

    await client.request({
      method: 'POST',
      url: '/files',
      body: new Uint8Array([1]),
      onUploadProgress: uploadProgress,
      onDownloadProgress: downloadProgress,
    });

    expect(uploadProgress).toHaveBeenCalledWith({
      loaded: 4,
      total: 10,
      progress: 0.4,
      bytesPerSecond: 20,
      bytes: 4,
      lengthComputable: undefined,
    });
    expect(downloadProgress).toHaveBeenCalledWith({
      loaded: 10,
      total: 10,
      progress: 1,
      bytesPerSecond: 30,
      bytes: 6,
      lengthComputable: undefined,
    });
    expect(uploadProgress.mock.calls[0]?.[0]).not.toHaveProperty('rate');
    expect(uploadProgress.mock.calls[0]?.[0]).toMatchObject({ bytes: 4 });
  });

  it('allows upload requests to override total and inactivity timeout semantics', async () => {
    const client = createMangoHttpClient({ baseUrl: '/api' });
    mocks.instances[0].request.mockResolvedValue(successResponse({ uploaded: true }));

    await expect(
      client.request({
        method: 'POST',
        url: '/file/files',
        body: new FormData(),
        timeoutMs: 10 * 60 * 1000,
        inactivityTimeoutMs: 60 * 1000,
      }),
    ).resolves.toEqual({ uploaded: true });

    expect(mocks.instances[0].request).toHaveBeenCalledWith(
      expect.objectContaining({
        timeout: 10 * 60 * 1000,
        onUploadProgress: expect.any(Function),
        onDownloadProgress: expect.any(Function),
      }),
    );
  });

  it('normalizes protocol failures without retaining Axios request, config or response objects', async () => {
    const client = createMangoHttpClient({ baseUrl: '/api' });
    mocks.instances[0].request.mockRejectedValue(
      axiosFailure({
        status: 503,
        code: 'E_UPSTREAM',
        message: 'service unavailable',
        body: { code: 'E_UPSTREAM', msg: '订单服务不可用', data: { operation: 'list' } },
        headers: { 'x-request-id': 'request-100' },
      }),
    );

    const failure = await client.request({ method: 'GET', url: '/orders' }).catch((error: unknown) => error);
    expect(failure).toBeInstanceOf(MangoHttpError);
    expect(failure).toMatchObject({
      kind: 'protocol',
      status: 503,
      code: 'E_UPSTREAM',
      requestId: 'request-100',
      retryable: true,
      details: { operation: 'list' },
    });
    expect(failure).not.toHaveProperty('config');
    expect(failure).not.toHaveProperty('request');
    expect(failure).not.toHaveProperty('response');
    expect(failure).not.toHaveProperty('cause');
  });

  it('rejects an HTTP 200 business failure when the envelope omits data', async () => {
    const client = createMangoHttpClient({ baseUrl: '/api' });
    mocks.instances[0].request.mockResolvedValue({
      status: 200,
      headers: { 'x-trace-id': 'trace-200' },
      data: { code: 3406, msg: '文件大小超过限制' },
    });

    await expect(client.request({ method: 'POST', url: '/files', body: {} })).rejects.toMatchObject({
      kind: 'protocol',
      status: 200,
      code: '3406',
      message: '文件大小超过限制',
      requestId: 'trace-200',
    });
  });

  it('preserves code and message from an HTTP error envelope that omits data', async () => {
    const client = createMangoHttpClient({ baseUrl: '/api' });
    mocks.instances[0].request.mockRejectedValue(
      axiosFailure({
        status: 400,
        message: 'Request failed with status code 400',
        body: { code: 400, msg: '请求体格式错误' },
      }),
    );

    await expect(client.request({ method: 'POST', url: '/orders', body: {} })).rejects.toMatchObject({
      kind: 'protocol',
      status: 400,
      code: '400',
      message: '请求体格式错误',
    });
  });

  it('rejects absolute business endpoints before dispatch', async () => {
    const client = createMangoHttpClient({ baseUrl: '/api' });
    await expect(client.request({ method: 'GET', url: 'https://example.com/orders' })).rejects.toMatchObject({
      kind: 'configuration',
    });
    expect(mocks.instances[0].request).not.toHaveBeenCalled();
  });

  it('forwards the grant issued by a list response to the following detail request', async () => {
    const client = mangoClientWithIdentity();
    mocks.instances[0].request
      .mockResolvedValueOnce(grantResponse('grant-a', 'fixture:item', [{ id: 1 }]))
      .mockResolvedValueOnce(successResponse({ id: 1 }));

    await client.request({ method: 'POST', url: '/fixture/items/page', body: {} });
    await client.request({ method: 'GET', url: '/fixture/items/detail', query: { itemId: 1 } });

    expect(requestHeaders(0)).not.toHaveProperty('X-Mango-Data-Grant');
    expect(requestHeaders(1)).toMatchObject({ 'X-Mango-Data-Grant': 'grant-a' });
  });

  it('sends every cached domain as a multi-value header', async () => {
    const client = mangoClientWithIdentity();
    mocks.instances[0].request
      .mockResolvedValueOnce(grantResponse('grant-item', 'fixture:item', [{ id: 1 }]))
      .mockResolvedValueOnce(grantResponse('grant-order', 'fixture:order', [{ id: 2 }]))
      .mockResolvedValueOnce(successResponse({ id: 1 }));

    await client.request({ method: 'POST', url: '/fixture/items/page', body: {} });
    await client.request({ method: 'POST', url: '/fixture/orders/page', body: {} });
    await client.request({ method: 'GET', url: '/fixture/items/detail' });

    expect(requestHeaders(2)['X-Mango-Data-Grant']).toBe('grant-order, grant-item');
  });

  it('drops the cached grant when the backend reports it absent', async () => {
    const client = mangoClientWithIdentity();
    mocks.instances[0].request
      .mockResolvedValueOnce(grantResponse('grant-a', 'fixture:item', [{ id: 1 }]))
      .mockRejectedValueOnce(
        axiosFailure({
          status: 403,
          message: 'forbidden',
          headers: {
            'x-mango-data-grant-domain': 'fixture:item',
            'x-mango-data-grant': 'absent',
            'x-mango-data-grant-required': '1',
          },
          body: { code: 403, message: '缺少数据权限凭证，请刷新列表后重试' },
        }),
      )
      .mockResolvedValueOnce(successResponse({ id: 1 }));

    await client.request({ method: 'POST', url: '/fixture/items/page', body: {} });
    await expect(client.request({ method: 'GET', url: '/fixture/items/detail' })).rejects.toBeInstanceOf(
      MangoHttpError,
    );
    await client.request({ method: 'GET', url: '/fixture/items/detail' });

    expect(requestHeaders(1)['X-Mango-Data-Grant']).toBe('grant-a');
    expect(requestHeaders(2)).not.toHaveProperty('X-Mango-Data-Grant');
  });

  it('ignores a late response that carries an older grant sequence', async () => {
    const client = mangoClientWithIdentity();
    let resolveSlow: ((value: unknown) => void) | undefined;
    mocks.instances[0].request
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            resolveSlow = resolve;
          }),
      )
      .mockResolvedValueOnce(grantResponse('grant-new', 'fixture:item', [{ id: 2 }]))
      .mockResolvedValue(successResponse({ id: 1 }));

    const slow = client.request({ method: 'POST', url: '/fixture/items/page', body: {} });
    await client.request({ method: 'POST', url: '/fixture/items/page', body: {} });
    resolveSlow?.(grantResponse('grant-old', 'fixture:item', [{ id: 1 }]));
    await slow;
    await client.request({ method: 'GET', url: '/fixture/items/detail' });

    expect(requestHeaders(2)['X-Mango-Data-Grant']).toBe('grant-new');
  });

  it('never attaches a grant to stream requests', async () => {
    const client = mangoClientWithIdentity();
    mocks.instances[0].request
      .mockResolvedValueOnce(grantResponse('grant-a', 'fixture:item', [{ id: 1 }]))
      .mockResolvedValueOnce({ status: 200, headers: {}, data: undefined });

    await client.request({ method: 'POST', url: '/fixture/items/page', body: {} });
    await client.request({ method: 'GET', url: '/files/1/download', responseType: 'stream' });

    expect(requestHeaders(1)).not.toHaveProperty('X-Mango-Data-Grant');
  });

  it('does not attach grants to requests that skip auth', async () => {
    const client = mangoClientWithIdentity();
    mocks.instances[0].request
      .mockResolvedValueOnce(grantResponse('grant-a', 'fixture:item', [{ id: 1 }]))
      .mockResolvedValueOnce(successResponse({ token: 'next' }));

    await client.request({ method: 'POST', url: '/fixture/items/page', body: {} });
    await client.request({
      method: 'POST',
      url: '/auth/refresh',
      body: {},
      metadata: { auth: 'none' },
    });

    expect(requestHeaders(1)).not.toHaveProperty('X-Mango-Data-Grant');
  });
});

function mangoClientWithIdentity() {
  return createMangoHttpClient({
    baseUrl: '/api',
    getAccessToken: () => 'token',
    getTenantId: () => '1',
    getUserId: () => '10023',
  });
}

function grantResponse(grant: string, domain: string, data: unknown) {
  return {
    status: 200,
    headers: {
      'x-mango-data-grant': grant,
      'x-mango-data-grant-domain': domain,
    },
    data: { code: 200, success: true, data },
  };
}

function requestHeaders(index: number): Record<string, string> {
  return mocks.instances[0].request.mock.calls[index][0].headers;
}

function successResponse(data: unknown) {
  return {
    status: 200,
    headers: {},
    data: { code: 200, success: true, data },
  };
}

function axiosFailure(options: {
  body?: unknown;
  code?: string;
  headers?: Record<string, string>;
  message: string;
  status?: number;
}) {
  return {
    isAxiosError: true,
    code: options.code,
    message: options.message,
    config: { url: '/internal' },
    request: {},
    response:
      options.status === undefined
        ? undefined
        : {
            status: options.status,
            headers: options.headers ?? {},
            data: options.body,
          },
  };
}
