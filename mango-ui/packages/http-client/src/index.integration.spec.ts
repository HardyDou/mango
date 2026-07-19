import { createServer, type Server } from 'node:http';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { createMangoHttpClient } from './index';

describe('Mango HTTP client Axios integration', () => {
  let baseUrl = '';
  let server: Server;
  let receivedHeaders: Record<string, string | string[] | undefined> = {};

  beforeAll(async () => {
    server = createServer((request, response) => {
      receivedHeaders = request.headers;
      response.setHeader('Content-Type', 'application/json');

      if (request.url?.startsWith('/success')) {
        response.end(JSON.stringify({ code: 200, data: { ok: true, url: request.url } }));
        return;
      }
      if (request.url === '/failure') {
        response.statusCode = 503;
        response.setHeader('X-Request-Id', 'integration-request-1');
        response.end(JSON.stringify({ code: 'E_UPSTREAM', message: 'service unavailable' }));
        return;
      }
      if (request.url === '/slow') {
        const timer = setTimeout(() => {
          if (!response.destroyed) response.end(JSON.stringify({ code: 200, data: { late: true } }));
        }, 1_000);
        request.once('close', () => clearTimeout(timer));
        return;
      }

      response.statusCode = 404;
      response.end(JSON.stringify({ code: 404, message: 'not found' }));
    });
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
    const address = server.address();
    if (!address || typeof address === 'string') throw new Error('Test server did not expose a TCP address');
    baseUrl = `http://127.0.0.1:${address.port}`;
  });

  afterAll(async () => {
    await new Promise<void>((resolve, reject) => {
      server.close((error) => (error ? reject(error) : resolve()));
    });
  });

  it('uses real Axios transport for headers, query serialization and envelope decoding', async () => {
    const client = createMangoHttpClient({
      baseUrl,
      getAccessToken: () => 'integration-token',
      getTenantId: () => 'integration-tenant',
      getTraceId: () => 'integration-trace',
    });

    await expect(
      client.request<{ ok: boolean; url: string }>({
        method: 'GET',
        url: '/success',
        query: { page: 2, keyword: 'mango' },
      }),
    ).resolves.toEqual({ ok: true, url: '/success?page=2&keyword=mango' });
    expect(receivedHeaders.authorization).toBe('Bearer integration-token');
    expect(receivedHeaders['x-mango-tenant-id']).toBe('integration-tenant');
    expect(receivedHeaders['tenant-id']).toBe('integration-tenant');
    expect(receivedHeaders['x-trace-id']).toBe('integration-trace');
  });

  it('normalizes a real non-2xx response without a data field', async () => {
    const client = createMangoHttpClient({ baseUrl });

    await expect(client.request({ method: 'GET', url: '/failure' })).rejects.toMatchObject({
      name: 'HttpError',
      kind: 'protocol',
      status: 503,
      code: 'E_UPSTREAM',
      requestId: 'integration-request-1',
      retryable: true,
    });
  });

  it('propagates AbortSignal cancellation through real Axios transport', async () => {
    const client = createMangoHttpClient({ baseUrl });
    const controller = new AbortController();
    const pending = client.request({ method: 'GET', url: '/slow', signal: controller.signal });
    controller.abort('route changed');

    await expect(pending).rejects.toMatchObject({ name: 'HttpError', kind: 'aborted', retryable: false });
  });
});
