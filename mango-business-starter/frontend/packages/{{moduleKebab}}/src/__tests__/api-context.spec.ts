import { createApp } from 'vue';
import type { HttpClient, HttpRequest } from '@mango/api-schema';
import { MANGO_HTTP_CLIENT_KEY } from '@mango/app-runtime';
import { describe, expect, it } from 'vitest';
import { use{{aggregatePascal}}Api } from '../api-context';

function createRecordingClient(requests: HttpRequest[]): HttpClient {
  return {
    async request<TResponse = unknown, TBody = unknown>(request: HttpRequest<TBody>): Promise<TResponse> {
      requests.push(request as HttpRequest);
      return {
        records: [],
        total: 0,
        page: 1,
        size: 20,
        pages: 0,
      } as TResponse;
    },
  };
}

describe('use{{aggregatePascal}}Api', () => {
  it('keeps HttpClient instances isolated between Vue apps', async () => {
    const firstRequests: HttpRequest[] = [];
    const secondRequests: HttpRequest[] = [];
    const firstApp = createApp({ render: () => null });
    const secondApp = createApp({ render: () => null });
    firstApp.provide(MANGO_HTTP_CLIENT_KEY, createRecordingClient(firstRequests));
    secondApp.provide(MANGO_HTTP_CLIENT_KEY, createRecordingClient(secondRequests));

    const firstApi = firstApp.runWithContext(() => use{{aggregatePascal}}Api());
    const secondApi = secondApp.runWithContext(() => use{{aggregatePascal}}Api());

    await firstApi.page({ page: 1, size: 20, name: 'first' });
    await secondApi.page({ page: 2, size: 10, name: 'second' });

    expect(firstApi).not.toBe(secondApi);
    expect(firstRequests).toHaveLength(1);
    expect(secondRequests).toHaveLength(1);
    expect(firstRequests[0]?.query).toEqual({ page: 1, size: 20, name: 'first' });
    expect(secondRequests[0]?.query).toEqual({ page: 2, size: 10, name: 'second' });
  });

  it('fails closed when the current Vue app has no HttpClient provider', () => {
    const app = createApp({ render: () => null });

    expect(() => app.runWithContext(() => use{{aggregatePascal}}Api())).toThrow(
      '{{modulePascal}} pages require an HttpClient provided by the current Vue app',
    );
  });
});
