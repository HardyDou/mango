import { describe, expect, it, vi } from 'vitest';
import { createServer } from 'node:http';
import { once } from 'node:events';
import type { HttpClient, HttpRequest } from '@mango/api-schema';
import { createMangoHttpClient } from '@mango/http-client';
import { createCmsApi } from '../api/cms';

describe('CMS API HttpClient boundary', () => {
  it('keeps two runtime clients and their request context independent', async () => {
    const first = recordingClient({ list: [], total: 0, page: 1, size: 10 });
    const second = recordingClient({ list: [], total: 0, page: 1, size: 20 });

    await createCmsApi(first.client).pageSites({ pageNum: 1, pageSize: 10 });
    await createCmsApi(second.client).pageSites({ pageNum: 1, pageSize: 20 });

    expect(first.requests).toEqual([expect.objectContaining({ query: { page: 1, size: 10 } })]);
    expect(second.requests).toEqual([expect.objectContaining({ query: { page: 1, size: 20 } })]);
    expect(first.client.request).toHaveBeenCalledTimes(1);
    expect(second.client.request).toHaveBeenCalledTimes(1);
  });

  it('keeps token and tenant headers isolated through the real Axios adapter', async () => {
    const received: Array<{ authorization?: string; tenantId?: string }> = [];
    const server = createServer((request, response) => {
      received.push({
        authorization: request.headers.authorization,
        tenantId: request.headers['x-mango-tenant-id'] as string | undefined,
      });
      response.setHeader('content-type', 'application/json');
      response.end(
        JSON.stringify({
          code: 200,
          success: true,
          data: { list: [], total: 0, page: 1, size: 10 },
        }),
      );
    });
    server.listen(0, '127.0.0.1');
    await once(server, 'listening');
    const address = server.address();
    if (!address || typeof address === 'string') throw new Error('Test server address is unavailable');
    const baseUrl = `http://127.0.0.1:${address.port}`;
    const first = createMangoHttpClient({ baseUrl, getAccessToken: () => 'token-a', getTenantId: () => 'tenant-a' });
    const second = createMangoHttpClient({ baseUrl, getAccessToken: () => 'token-b', getTenantId: () => 'tenant-b' });

    try {
      await Promise.all([
        createCmsApi(first).pageSites({ pageNum: 1, pageSize: 10 }),
        createCmsApi(second).pageSites({ pageNum: 1, pageSize: 10 }),
      ]);
    } finally {
      first.destroy();
      second.destroy();
      server.close();
      await once(server, 'close');
    }

    expect(received).toEqual(
      expect.arrayContaining([
        { authorization: 'Bearer token-a', tenantId: 'tenant-a' },
        { authorization: 'Bearer token-b', tenantId: 'tenant-b' },
      ]),
    );
  });

  it('passes the page lifecycle AbortSignal to the real CMS request', async () => {
    const abortController = new AbortController();
    const pending = abortingClient();
    const request = createCmsApi(pending.client).pageSites(
      { pageNum: 1, pageSize: 10 },
      { signal: abortController.signal },
    );

    abortController.abort();

    await expect(request).rejects.toMatchObject({ name: 'AbortError' });
    expect(pending.requests[0].signal?.aborted).toBe(true);
  });
});

function recordingClient(result: unknown) {
  const requests: HttpRequest[] = [];
  const client: HttpClient = {
    request: vi.fn(async (request: HttpRequest) => {
      requests.push(request);
      return result;
    }) as HttpClient['request'],
  };
  return { client, requests };
}

function abortingClient() {
  const requests: HttpRequest[] = [];
  const client: HttpClient = {
    request: vi.fn((request: HttpRequest) => {
      requests.push(request);
      return new Promise((resolve, reject) => {
        request.signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')), {
          once: true,
        });
      });
    }) as HttpClient['request'],
  };
  return { client, requests };
}
