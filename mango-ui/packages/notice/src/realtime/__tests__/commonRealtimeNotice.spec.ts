import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createRealtimeClient, type RealtimeMessage } from '@mango/common/utils/realtime';

const originalFetch = globalThis.fetch;
const originalLocation = window.location;

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('common realtime notice dispatch', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    Object.defineProperty(window, 'location', {
      value: new URL('http://localhost/'),
      writable: true,
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    globalThis.fetch = originalFetch;
    Object.defineProperty(window, 'location', {
      value: originalLocation,
      writable: true,
    });
    vi.restoreAllMocks();
  });

  it('notice 业务事件不作为系统消息吞掉，能进入 notice 订阅', async () => {
    const received: RealtimeMessage[] = [];
    globalThis.fetch = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes('/polling')) {
        return jsonResponse([{
          id: 'notice-1',
          version: '1.0',
          event: { domain: 'default', name: 'notice' },
          payload: { type: 'text', text: '{"messageId":"1001","title":"系统消息"}' },
        }]);
      }
      return jsonResponse(undefined);
    }) as typeof fetch;

    const client = createRealtimeClient({
      mode: 'polling',
      heartbeat: { suppressEvents: true },
      polling: { timeoutMillis: 0, interval: 1000 },
    });
    client.subscribe('notice', message => received.push(message));

    await client.connect();
    await vi.runOnlyPendingTimersAsync();

    expect(received).toEqual([
      expect.objectContaining({
        id: 'notice-1',
        event: { domain: 'default', name: 'notice' },
        payload: { type: 'text', text: '{"messageId":"1001","title":"系统消息"}' },
      }),
    ]);
    client.disconnect();
  });
});
