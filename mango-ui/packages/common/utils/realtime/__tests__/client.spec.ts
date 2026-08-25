import { afterEach, describe, expect, it, vi } from 'vitest';

import { MangoRealtimeClient } from '../client';
import type { RealtimeProtocol } from '../types';

describe('MangoRealtimeClient ticket transport authentication', () => {
  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
    sessionStorage.clear();
  });

  it('uses the negotiated ticket for the WebSocket probe and formal connection', async () => {
    const urls: string[] = [];
    vi.stubGlobal('fetch', negotiationFetch('ticket-websocket', 'websocket'));
    vi.stubGlobal('WebSocket', websocketStub(urls));

    const client = new MangoRealtimeClient({ heartbeat: false });
    await client.connect();

    expect(urls).toHaveLength(2);
    expectTicketUrl(urls[0], '/api/realtime/transports/probe/websocket', 'ticket-websocket');
    expectTicketUrl(urls[1], '/api/realtime/transports/websocket', 'ticket-websocket');
    expect(new URL(urls[1]).searchParams.get('clientId')).toBe(client.getClientId());
    expect(client.getStatus()).toBe('connected');
    client.disconnect();
  });

  it('uses the negotiated ticket for the SSE probe and formal connection', async () => {
    const urls: string[] = [];
    vi.stubGlobal('fetch', negotiationFetch('ticket-sse', 'sse'));
    vi.stubGlobal('EventSource', eventSourceStub(urls));

    const client = new MangoRealtimeClient({ heartbeat: false });
    await client.connect();

    expect(urls).toHaveLength(2);
    expectTicketUrl(urls[0], '/api/realtime/transports/probe/sse', 'ticket-sse');
    expectTicketUrl(urls[1], '/api/realtime/transports/sse', 'ticket-sse');
    expect(new URL(urls[1]).searchParams.get('clientId')).toBe(client.getClientId());
    expect(client.getStatus()).toBe('connected');
    client.disconnect();
  });

  it('keeps explicit transport mode on its token or cookie authentication path', async () => {
    const urls: string[] = [];
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('WebSocket', websocketStub(urls, false));

    const client = new MangoRealtimeClient({ mode: 'websocket', heartbeat: false });
    await client.connect();

    expect(urls).toHaveLength(1);
    expect(new URL(urls[0]).pathname).toBe('/api/realtime/transports/websocket');
    expect(new URL(urls[0]).searchParams.has('rtTicket')).toBe(false);
    expect(fetchMock).not.toHaveBeenCalled();
    client.disconnect();
  });

  it('renegotiates before an automatic WebSocket reconnect', async () => {
    vi.useFakeTimers();
    const urls: string[] = [];
    const tickets = ['ticket-initial', 'ticket-reconnect'];
    const fetchMock = vi.fn(async () => responseForNegotiation(tickets.shift() || 'unexpected', 'websocket'));
    const WebSocketStub = websocketStub(urls);
    vi.stubGlobal('fetch', fetchMock);
    vi.stubGlobal('WebSocket', WebSocketStub);
    const client = new MangoRealtimeClient({
      heartbeat: false,
      reconnect: { minDelay: 1, maxDelay: 1, jitter: false },
      transportPolicy: { downgrade: { enabled: false } },
    });
    await client.connect();

    WebSocketStub.instances[1]?.fail();
    await vi.advanceTimersByTimeAsync(1);
    await vi.waitFor(() => expect(client.getStatus()).toBe('connected'));

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(urls).toHaveLength(4);
    expectTicketUrl(urls[3], '/api/realtime/transports/websocket', 'ticket-reconnect');
    client.disconnect();
  });
});

function negotiationFetch(ticket: string, protocol: RealtimeProtocol) {
  return vi.fn(async () => responseForNegotiation(ticket, protocol));
}

function responseForNegotiation(ticket: string, protocol: RealtimeProtocol): Response {
  return new Response(JSON.stringify({
    recommended: protocol,
    order: [protocol],
    transports: [{
      type: protocol,
      enabled: true,
      available: true,
      probeRequired: true,
    }],
    connectionTicket: ticket,
    ticketExpiresAt: Date.now() + 60_000,
  }), {
    status: 200,
    headers: { 'Content-Type': 'application/json' },
  });
}

function websocketStub(urls: string[], probeFirst = true) {
  class WebSocketStub {
    static readonly CONNECTING = 0;
    static readonly OPEN = 1;
    static readonly CLOSED = 3;
    static readonly instances: WebSocketStub[] = [];

    readonly url: string;
    readyState = WebSocketStub.CONNECTING;
    onopen: (() => void) | null = null;
    onmessage: ((event: { data: string }) => void) | null = null;
    onerror: (() => void) | null = null;
    onclose: ((event: { code: number }) => void) | null = null;

    constructor(url: string | URL) {
      this.url = String(url);
      urls.push(this.url);
      WebSocketStub.instances.push(this);
      const probe = probeFirst && WebSocketStub.instances.length % 2 === 1;
      queueMicrotask(() => {
        this.readyState = WebSocketStub.OPEN;
        if (probe) {
          this.onmessage?.({ data: '{"type":"probe.ok"}' });
        } else {
          this.onopen?.();
        }
      });
    }

    close() {
      this.readyState = WebSocketStub.CLOSED;
      this.onclose?.({ code: 1000 });
    }

    send() {
      // The connection URL is the behavior under test.
    }

    fail() {
      this.readyState = WebSocketStub.CLOSED;
      this.onclose?.({ code: 1006 });
    }
  }

  return WebSocketStub;
}

function eventSourceStub(urls: string[]) {
  return class EventSourceStub {
    static instances = 0;

    onopen: (() => void) | null = null;
    onmessage: ((event: { data: string }) => void) | null = null;
    onerror: (() => void) | null = null;

    constructor(url: string | URL) {
      urls.push(String(url));
      EventSourceStub.instances += 1;
      const probe = EventSourceStub.instances % 2 === 1;
      queueMicrotask(() => {
        if (probe) {
          this.onmessage?.({ data: '{"type":"probe.ok"}' });
        } else {
          this.onopen?.();
        }
      });
    }

    close() {
      // No resources are allocated by the test transport.
    }
  };
}

function expectTicketUrl(urlValue: string, path: string, ticket: string) {
  const url = new URL(urlValue);
  expect(url.pathname).toBe(path);
  expect(url.searchParams.get('rtTicket')).toBe(ticket);
}
