import { describe, expect, it, vi } from 'vitest';
import type { HttpClient, HttpRequest } from '@mango/api-schema';
import { createInformationApi } from '../api/information';

describe('information API boundary', () => {
  it('maps admin page parameters to backend page and size', async () => {
    const requests: HttpRequest[] = [];
    const client: HttpClient = {
      request: vi.fn(async (request: HttpRequest) => {
        requests.push(request);
        return { list: [], total: 0, page: 2, size: 20 };
      }) as HttpClient['request'],
    };
    const result = await createInformationApi(client).pageForms({ pageNum: 2, pageSize: 20, keyword: 'contact' });
    expect(result).toMatchObject({ list: [], total: 0, pageNum: 2, pageSize: 20 });
    expect(requests[0]).toMatchObject({
      url: '/information/forms/page',
      query: { page: 2, size: 20, keyword: 'contact' },
    });
  });

  it('keeps the browser submit payload free of server secret', async () => {
    const client: HttpClient = {
      request: vi.fn(async () => ({ submissionNo: 'IC-1', message: 'ok' })) as HttpClient['request'],
    };
    await createInformationApi(client).submitPublic({ formCode: 'contact', values: { name: 'Mango' } });
    const request = vi.mocked(client.request).mock.calls[0]?.[0];
    expect(request?.body).toEqual({ formCode: 'contact', values: { name: 'Mango' } });
  });
});
