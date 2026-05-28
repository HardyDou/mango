import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createRealtimeClient, type RealtimeMessage } from '@mango/common';

const originalFetch = globalThis.fetch;
const originalLocation = window.location;

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

describe('createRealtimeClient', () => {
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

  it('auto 协商选择服务端推荐协议并通过 Polling 接收业务消息', async () => {
    const received: RealtimeMessage[] = [];
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      if (url.includes('/negotiate')) {
        return jsonResponse({
          recommended: 'polling',
          transports: [{ type: 'polling', enabled: true }],
        });
      }
      if (url.includes('/polling')) {
        return jsonResponse([{
          id: 'm1',
          version: '1.0',
          event: { domain: 'chat', name: 'message' },
          payload: { type: 'text', text: 'hello' },
        }]);
      }
      return jsonResponse(undefined);
    });
    globalThis.fetch = fetchMock as typeof fetch;

    const client = createRealtimeClient({
      mode: 'auto',
      heartbeat: false,
      polling: { timeoutMillis: 0, interval: 1000 },
    });
    client.subscribe('chat.message', message => received.push(message));

    await client.connect();
    await vi.runOnlyPendingTimersAsync();

    expect(client.getProtocol()).toBe('polling');
    expect(client.getStatus()).toBe('connected');
    expect(received).toEqual([
      expect.objectContaining({
        event: { domain: 'chat', name: 'message' },
        payload: { type: 'text', text: 'hello' },
      }),
    ]);
    client.disconnect();
  });

  it('Polling 上行使用 inbound HTTP 并携带统一消息格式', async () => {
    const bodies: unknown[] = [];
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.includes('/inbound/polling')) {
        bodies.push(JSON.parse(String(init?.body)));
        return jsonResponse({
          id: 'accepted-1',
          version: '1.0',
          event: { domain: 'system', name: 'message.accepted' },
          payload: { type: 'text', message: 'ok' },
        });
      }
      if (url.includes('/polling')) return jsonResponse([]);
      return jsonResponse(undefined);
    });
    globalThis.fetch = fetchMock as typeof fetch;

    const client = createRealtimeClient({
      mode: 'polling',
      identity: { tenantId: 'default', userId: 1001, clientId: 'unit-client' },
      heartbeat: false,
      polling: { timeoutMillis: 0, interval: 1000 },
    });

    await client.connect();
    await client.send({ event: { domain: 'chat', name: 'message' }, payload: { type: 'text', text: 'hello' } });

    expect(bodies).toEqual([
      expect.objectContaining({
        event: { domain: 'chat', name: 'message' },
        payload: { type: 'text', text: 'hello' },
        context: expect.objectContaining({ tenantId: 'default', userId: 1001 }),
        source: expect.objectContaining({ clientId: 'unit-client' }),
      }),
    ]);
    client.disconnect();
  });

  it('心跳间隔按性能模式钳制，普通模式最小 1000ms，aggressive 可到 500ms', async () => {
    const intervals: number[] = [];
    const setIntervalSpy = vi.spyOn(window, 'setInterval').mockImplementation(((handler: TimerHandler, timeout?: number) => {
      intervals.push(Number(timeout));
      return globalThis.setInterval(handler, timeout) as unknown as number;
    }) as typeof window.setInterval);
    globalThis.fetch = vi.fn(async (input: RequestInfo | URL) => {
      if (String(input).includes('/polling')) return jsonResponse([]);
      return jsonResponse({ id: 'accepted-1', type: 'accepted' });
    }) as typeof fetch;

    const normal = createRealtimeClient({ mode: 'polling', heartbeat: { interval: 1 }, polling: { timeoutMillis: 0 } });
    await normal.connect();
    normal.disconnect();

    const aggressive = createRealtimeClient({ mode: 'polling', performanceMode: 'aggressive', heartbeat: { interval: 1 }, polling: { timeoutMillis: 0 } });
    await aggressive.connect();
    aggressive.disconnect();

    expect(intervals).toContain(1000);
    expect(intervals).toContain(500);
    setIntervalSpy.mockRestore();
  });

  it('ping/pong 与心跳 accepted 不进入业务订阅', async () => {
    const received: RealtimeMessage[] = [];
    globalThis.fetch = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.includes('/polling')) {
        return jsonResponse([
          {
            id: 'pong-1',
            version: '1.0',
            event: { domain: 'system', name: 'heartbeat.pong' },
            payload: { type: 'heartbeat' },
          },
          {
            id: 'biz-1',
            version: '1.0',
            event: { domain: 'chat', name: 'message' },
            payload: { type: 'text', text: 'visible' },
          },
        ]);
      }
      if (url.includes('/inbound/polling')) {
        const body = JSON.parse(String(init?.body));
        return jsonResponse({
          id: 'accepted-heartbeat',
          version: '1.0',
          event: { domain: 'system', name: 'message.accepted' },
          metadata: body.metadata,
        });
      }
      return jsonResponse(undefined);
    }) as typeof fetch;

    const client = createRealtimeClient({
      mode: 'polling',
      heartbeat: { interval: 1000, timeout: 1000, suppressEvents: true },
      polling: { timeoutMillis: 0, interval: 1000 },
    });
    client.subscribe('*', message => received.push(message));

    await client.connect();
    await vi.runOnlyPendingTimersAsync();

    expect(received).toEqual([
      expect.objectContaining({
        event: { domain: 'chat', name: 'message' },
        payload: { type: 'text', text: 'visible' },
      }),
    ]);
    client.disconnect();
  });

});
