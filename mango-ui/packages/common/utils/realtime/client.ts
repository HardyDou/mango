import { Session } from '../storage';
import type {
  RealtimeAuthOptions,
  RealtimeClient,
  RealtimeEndpoints,
  RealtimeEvent,
  RealtimeEventHandler,
  RealtimeEventPayloadMap,
  RealtimeHeartbeatOptions,
  RealtimeIdentity,
  RealtimeMessage,
  RealtimeMessageHandler,
  RealtimeMetrics,
  RealtimeMode,
  RealtimeNegotiationResponse,
  RealtimeOptions,
  RealtimePollingOptions,
  RealtimeProtocol,
  RealtimeReconnectOptions,
  RealtimeStatus,
  RealtimeTransportPolicy,
  RealtimeUnsubscribe,
} from './types';
import { REALTIME_MESSAGE_KIND, REALTIME_SYSTEM_MESSAGE_KINDS } from './types';
import {
  createRealtimeEnvelope,
  decodeRealtimeEnvelope,
  isHeartbeatEnvelope,
  messageEventAliases,
  messageEventName,
  normalizeMessageKind,
  normalizeRealtimeEnvelope,
} from './envelope';

type EventMap = {
  [K in RealtimeEvent]?: Set<RealtimeEventHandler<K>>;
};

type RealtimeErrorScope = 'negotiate' | 'transport' | 'auth' | 'permission' | 'request';

class RealtimeConnectionError extends Error {
  constructor(
    message: string,
    readonly scope: RealtimeErrorScope,
    readonly status?: number,
  ) {
    super(message);
    this.name = 'RealtimeConnectionError';
  }
}

const DEFAULT_ENDPOINTS: Required<RealtimeEndpoints> = {
  negotiate: '/api/realtime/transports/negotiate',
  websocket: '/api/realtime/transports/websocket',
  sse: '/api/realtime/transports/sse',
  polling: '/api/realtime/transports/polling',
  probeWebsocket: '/api/realtime/transports/probe/websocket',
  probeSse: '/api/realtime/transports/probe/sse',
  probePolling: '/api/realtime/transports/probe/polling',
  inboundSse: '/api/realtime/messages/inbound/sse',
  inboundPolling: '/api/realtime/messages/inbound/polling',
};

const PROTOCOL_RANK: Record<RealtimeProtocol, number> = {
  websocket: 0,
  sse: 1,
  polling: 2,
};

const DEFAULT_RECONNECT: Required<RealtimeReconnectOptions> = {
  enabled: true,
  maxRetries: Infinity,
  minDelay: 1000,
  maxDelay: 30000,
  factor: 2,
  jitter: true,
};

const DEFAULT_HEARTBEAT: Required<Omit<RealtimeHeartbeatOptions, 'payload'>> = {
  enabled: true,
  interval: 30000,
  timeout: 5000,
  minInterval: 1000,
  hiddenInterval: 60000,
  missThreshold: 2,
  suppressEvents: true,
};

const DEFAULT_POLICY: Required<RealtimeTransportPolicy> = {
  adaptive: true,
  fallbackOrder: ['websocket', 'sse', 'polling'],
  downgrade: {
    enabled: true,
    onConnectFailure: true,
    onHeartbeatTimeout: true,
    consecutiveErrors: 2,
  },
  upgrade: {
    enabled: false,
    interval: 60000,
    onlyWhenIdle: true,
    target: 'websocket',
  },
};

const DEFAULT_POLLING: Required<RealtimePollingOptions> = {
  maxSize: 20,
  timeoutMillis: 25000,
  interval: 1000,
};

export class MangoRealtimeClient implements RealtimeClient {
  private readonly mode: RealtimeMode;
  private readonly endpoints: Required<RealtimeEndpoints>;
  private readonly identity: RealtimeIdentity;
  private readonly auth?: RealtimeAuthOptions;
  private readonly reconnectOptions: Required<RealtimeReconnectOptions>;
  private readonly heartbeatOptions: Required<Omit<RealtimeHeartbeatOptions, 'payload'>> & Pick<RealtimeHeartbeatOptions, 'payload'>;
  private readonly transportPolicy: Required<RealtimeTransportPolicy>;
  private readonly pollingOptions: Required<RealtimePollingOptions>;
  private readonly debug: boolean;

  private status: RealtimeStatus = 'idle';
  private protocol: RealtimeProtocol | null = null;
  private sessionId: string | null = null;
  private retryCount = 0;
  private consecutiveErrors = 0;
  private negotiatedOrder: RealtimeProtocol[] = [];
  private negotiation: RealtimeNegotiationResponse | null = null;
  private connectionTicket: string | null = null;
  private intentionalClose = false;
  private websocket: WebSocket | null = null;
  private eventSource: EventSource | null = null;
  private pollingAbortController: AbortController | null = null;
  private pollingActive = false;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  private heartbeatTimeoutTimer: ReturnType<typeof setTimeout> | null = null;
  private upgradeTimer: ReturnType<typeof setInterval> | null = null;
  private subscribers = new Map<string, Set<RealtimeMessageHandler>>();
  private eventHandlers: EventMap = {};
  private seenMessages = new Map<string, number>();
  private sequence = 10000;
  private metrics: RealtimeMetrics = {
    protocol: null,
    status: 'idle',
    reconnectCount: 0,
    receivedCount: 0,
    sentCount: 0,
    heartbeatSentCount: 0,
    heartbeatMissCount: 0,
    transportSwitchCount: 0,
  };

  constructor(options: RealtimeOptions = {}) {
    this.mode = options.mode || 'auto';
    this.endpoints = { ...DEFAULT_ENDPOINTS, ...(options.endpoints || {}) };
    this.identity = {
      tenantId: Session.get('tenantId') || Session.get('userInfo')?.tenantId || 'default',
      userId: Session.get('userInfo')?.id,
      clientId: createClientId(),
      ...options.identity,
    };
    this.auth = options.auth;
    this.reconnectOptions = normalizeReconnect(options.reconnect);
    this.heartbeatOptions = normalizeHeartbeat(options.heartbeat, options.performanceMode);
    this.transportPolicy = normalizeTransportPolicy(options.transportPolicy);
    this.pollingOptions = { ...DEFAULT_POLLING, ...(options.polling || {}) };
    this.debug = Boolean(options.debug);

    if (options.autoConnect) {
      void this.connect();
    }
  }

  async connect(): Promise<void> {
    this.intentionalClose = false;
    this.clearReconnectTimer();
    this.stopActiveTransport();
    this.retryCount = 0;
    this.consecutiveErrors = 0;
    this.setStatus(this.retryCount > 0 ? 'reconnecting' : 'connecting');

    try {
      const protocol = await this.resolveInitialProtocol();
      await this.connectProtocol(protocol, 'connect');
    } catch (error) {
      await this.handleConnectFailure(normalizeError(error));
    }
  }

  disconnect(reason = 'manual'): void {
    this.intentionalClose = true;
    this.retryCount = 0;
    this.clearReconnectTimer();
    this.stopHeartbeat();
    this.stopUpgradeProbe();
    this.stopActiveTransport();
    this.protocol = null;
    this.metrics.protocol = null;
    this.setStatus('disconnected');
    this.emit('disconnected', { reason });
  }

  async reconnect(): Promise<void> {
    this.retryCount = 0;
    await this.connect();
  }

  async send(message: Partial<RealtimeMessage>): Promise<void> {
    if (!this.protocol) throw new Error('Realtime is not connected');
    const payload = this.buildMessage(message);
    if (isHeartbeatEnvelope(payload)) {
      await this.sendHeartbeatPayload(payload);
      return;
    }

    if (this.protocol === 'websocket') {
      if (!this.websocket || this.websocket.readyState !== WebSocket.OPEN) {
        throw new Error('WebSocket is not connected');
      }
      this.websocket.send(JSON.stringify(this.toWireMessage(payload)));
    } else {
      await this.postInbound(payload);
    }
    this.metrics.sentCount += 1;
    this.emit('sent', payload);
  }

  subscribe(type: string, handler: RealtimeMessageHandler): RealtimeUnsubscribe {
    const handlers = this.subscribers.get(type) || new Set<RealtimeMessageHandler>();
    handlers.add(handler);
    this.subscribers.set(type, handlers);
    return () => this.unsubscribe(type, handler);
  }

  once(type: string, handler: RealtimeMessageHandler): RealtimeUnsubscribe {
    const off = this.subscribe(type, (message) => {
      off();
      handler(message);
    });
    return off;
  }

  unsubscribe(type: string, handler: RealtimeMessageHandler): void {
    const handlers = this.subscribers.get(type);
    if (!handlers) return;
    handlers.delete(handler);
    if (handlers.size === 0) this.subscribers.delete(type);
  }

  on<T extends RealtimeEvent>(event: T, handler: RealtimeEventHandler<T>): RealtimeUnsubscribe {
    const handlers = this.eventHandlers[event] || new Set();
    handlers.add(handler as never);
    this.eventHandlers[event] = handlers as never;
    return () => this.off(event, handler);
  }

  off<T extends RealtimeEvent>(event: T, handler: RealtimeEventHandler<T>): void {
    this.eventHandlers[event]?.delete(handler as never);
  }

  getStatus(): RealtimeStatus {
    return this.status;
  }

  getProtocol(): RealtimeProtocol | null {
    return this.protocol;
  }

  getSessionId(): string | null {
    return this.sessionId;
  }

  getClientId(): string | undefined {
    return this.identity.clientId;
  }

  getMetrics(): RealtimeMetrics {
    return { ...this.metrics };
  }

  private async resolveInitialProtocol(): Promise<RealtimeProtocol> {
    if (this.mode !== 'auto') {
      this.negotiatedOrder = [this.mode];
      return this.mode;
    }

    const url = new URL(this.endpoints.negotiate, window.location.origin);
    url.searchParams.set('prefer', this.transportPolicy.fallbackOrder.join(','));
    url.searchParams.set('clientWebSocket', String(typeof WebSocket !== 'undefined'));
    url.searchParams.set('clientSse', String(typeof EventSource !== 'undefined'));
    url.searchParams.set('clientPolling', String(typeof fetch !== 'undefined'));
    url.searchParams.set('secureContext', String(window.isSecureContext));
    url.searchParams.set('pageProtocol', window.location.protocol.replace(':', ''));
    url.searchParams.set('wsTokenAvailable', String(Boolean(await this.resolveToken())));
    url.searchParams.set('sseTokenAvailable', String(Boolean(await this.resolveToken())));
    url.searchParams.set('cookieAvailable', String(typeof navigator !== 'undefined' && navigator.cookieEnabled));
    let result: RealtimeNegotiationResponse;
    try {
      result = await this.fetchJson<RealtimeNegotiationResponse>(url.toString());
    } catch (error) {
      throw classifyRequestError(error, 'negotiate');
    }
    this.emit('negotiated', result);
    this.negotiation = result;
    this.connectionTicket = result.connectionTicket || null;

    const enabled = new Set((result.transports || [])
      .filter(item => item.enabled && item.available !== false)
      .map(item => item.type));
    const order = uniqueProtocols([
      ...(result.order || []),
      ...(result.recommended ? [result.recommended] : []),
      ...this.transportPolicy.fallbackOrder,
    ]).filter(protocol => enabled.size === 0 || enabled.has(protocol));
    this.negotiatedOrder = order.length > 0 ? order : ['polling'];
    const probed = await this.probeNegotiatedTransports(this.negotiatedOrder);
    if (probed) return probed;
    const recommended = result.recommended && enabled.has(result.recommended) ? result.recommended : null;
    return recommended || this.negotiatedOrder[0] || 'polling';
  }

  private async probeNegotiatedTransports(order: RealtimeProtocol[]): Promise<RealtimeProtocol | null> {
    for (const protocol of order) {
      const capability = this.negotiation?.transports?.find(item => item.type === protocol);
      if (!capability?.probeRequired) return protocol;
      try {
        await this.probeTransport(protocol);
        return protocol;
      } catch (error) {
        this.warn(`Realtime ${protocol} probe failed: ${normalizeError(error).message}`);
      }
    }
    return null;
  }

  private async probeTransport(protocol: RealtimeProtocol, endpoint?: string): Promise<void> {
    if (protocol === 'websocket') {
      await this.probeWebSocket(endpoint || this.endpoints.probeWebsocket);
      return;
    }
    if (protocol === 'sse') {
      await this.probeSse(endpoint || this.endpoints.probeSse);
      return;
    }
    const url = new URL(endpoint || this.endpoints.probePolling, window.location.origin);
    await this.fetchJson(url.toString());
  }

  private async probeWebSocket(endpoint: string): Promise<void> {
    const url = this.ticketUrl(endpoint, 'websocket');
    await new Promise<void>((resolve, reject) => {
      const websocket = new WebSocket(url.toString());
      let settled = false;
      const timeout = window.setTimeout(() => {
        if (settled) return;
        settled = true;
        websocket.close();
        reject(new Error('WebSocket probe timeout'));
      }, 5000);
      websocket.onmessage = () => {
        if (settled) return;
        settled = true;
        window.clearTimeout(timeout);
        websocket.close();
        resolve();
      };
      websocket.onopen = () => undefined;
      websocket.onerror = () => {
        if (!settled) {
          settled = true;
          window.clearTimeout(timeout);
          reject(new Error('WebSocket probe error'));
        }
      };
      websocket.onclose = event => {
        if (!settled && event.code !== 1000) {
          settled = true;
          window.clearTimeout(timeout);
          reject(new Error(`WebSocket probe closed: ${event.code}`));
        }
      };
    });
  }

  private async probeSse(endpoint: string): Promise<void> {
    const url = this.ticketUrl(endpoint, 'sse');
    await new Promise<void>((resolve, reject) => {
      const eventSource = new EventSource(url.toString());
      let settled = false;
      const timeout = window.setTimeout(() => {
        if (settled) return;
        settled = true;
        eventSource.close();
        reject(new Error('SSE probe timeout'));
      }, 5000);
      eventSource.onmessage = () => {
        if (settled) return;
        settled = true;
        window.clearTimeout(timeout);
        eventSource.close();
        resolve();
      };
      eventSource.onerror = () => {
        if (!settled) {
          settled = true;
          window.clearTimeout(timeout);
          eventSource.close();
          reject(new Error('SSE probe error'));
        }
      };
    });
  }

  private async connectProtocol(protocol: RealtimeProtocol, reason: string): Promise<void> {
    const previous = this.protocol;
    this.protocol = protocol;
    this.metrics.protocol = protocol;
    if (previous !== protocol) {
      this.metrics.transportSwitchCount += previous ? 1 : 0;
      this.emit('transport-change', { from: previous, to: protocol, reason });
    }

    if (protocol === 'websocket') {
      await this.openWebSocket();
      return;
    }
    if (protocol === 'sse') {
      await this.openSse();
      return;
    }
    this.openPolling();
  }

  private async openWebSocket(): Promise<void> {
    const url = this.transportUrl(this.endpoints.websocket, 'websocket');

    await new Promise<void>((resolve, reject) => {
      const websocket = new WebSocket(url.toString());
      let settled = false;
      const timeout = window.setTimeout(() => {
        if (settled) return;
        settled = true;
        websocket.close();
        reject(new Error('WebSocket connect timeout'));
      }, 10000);

      websocket.onopen = () => {
        if (settled) return;
        settled = true;
        window.clearTimeout(timeout);
        this.websocket = websocket;
        this.markConnected('websocket');
        resolve();
      };
      websocket.onmessage = event => this.receiveMessage(this.parseMessage(event.data));
      websocket.onerror = () => {
        if (!settled) {
          settled = true;
          window.clearTimeout(timeout);
          reject(new Error('WebSocket connection error'));
        }
      };
      websocket.onclose = () => {
        if (this.websocket !== websocket) return;
        if (this.websocket === websocket) this.websocket = null;
        if (!this.intentionalClose) this.scheduleReconnectOrDowngrade('close');
      };
    });
  }

  private async openSse(): Promise<void> {
    const url = this.transportUrl(this.endpoints.sse, 'sse');

    await new Promise<void>((resolve, reject) => {
      const eventSource = new EventSource(url.toString());
      let settled = false;
      const timeout = window.setTimeout(() => {
        if (settled) return;
        settled = true;
        eventSource.close();
        reject(new Error('SSE connect timeout'));
      }, 10000);

      eventSource.onopen = () => {
        if (!settled) {
          settled = true;
          window.clearTimeout(timeout);
          this.eventSource = eventSource;
          this.markConnected('sse');
          resolve();
        }
      };
      eventSource.onmessage = event => this.receiveMessage(this.parseMessage(event.data));
      eventSource.onerror = () => {
        if (!settled) {
          settled = true;
          window.clearTimeout(timeout);
          eventSource.close();
          reject(new Error('SSE connection error'));
          return;
        }
        eventSource.close();
        if (this.eventSource === eventSource) this.eventSource = null;
        if (!this.intentionalClose) this.scheduleReconnectOrDowngrade('sse-error');
      };
    });
  }

  private openPolling(): void {
    this.pollingActive = true;
    this.markConnected('polling');
    void this.pollLoop();
  }

  private async pollLoop(): Promise<void> {
    while (this.pollingActive) {
      this.pollingAbortController = new AbortController();
      const url = new URL(this.endpoints.polling, window.location.origin);
      url.searchParams.set('maxSize', String(this.pollingOptions.maxSize));
      url.searchParams.set('timeoutMillis', String(this.pollingOptions.timeoutMillis));
      if (this.identity.clientId) {
        url.searchParams.set('clientId', this.identity.clientId);
      }

      try {
        const messages = await this.fetchJson<RealtimeMessage[]>(url.toString(), {
          signal: this.pollingAbortController.signal,
        });
        messages.forEach(message => this.receiveMessage(message));
        if (messages.length === 0 || this.pollingOptions.timeoutMillis === 0) {
          await sleep(this.pollingOptions.interval);
        }
      } catch (error) {
        if (!this.pollingActive) return;
        this.handleRuntimeError(normalizeError(error));
        return;
      }
    }
  }

  private markConnected(protocol: RealtimeProtocol): void {
    this.consecutiveErrors = 0;
    const wasReconnecting = this.retryCount > 0;
    this.sessionId = this.identity.sessionId || `${protocol}-${this.identity.clientId || createClientId()}`;
    this.setStatus('connected');
    this.metrics.lastConnectedAt = Date.now();
    this.emit('connected', { protocol });
    if (wasReconnecting) {
      this.emit('reconnected', { protocol, retryCount: this.retryCount });
    }
    this.retryCount = 0;
    this.startHeartbeat();
    this.startUpgradeProbe();
  }

  private async handleConnectFailure(error: Error): Promise<void> {
    this.emit('error', error);
    if (!isDowngradableError(error)) {
      this.stopHeartbeat();
      this.stopActiveTransport();
      this.setStatus('error');
      return;
    }
    this.consecutiveErrors += 1;
    this.scheduleReconnectOrDowngrade(error.message);
  }

  private handleRuntimeError(error: Error): void {
    this.emit('error', error);
    if (!isDowngradableError(error)) {
      this.stopHeartbeat();
      this.stopActiveTransport();
      this.setStatus('error');
      return;
    }
    this.consecutiveErrors += 1;
    this.scheduleReconnectOrDowngrade(error.message);
  }

  private scheduleReconnectOrDowngrade(reason: string): void {
    this.stopHeartbeat();
    this.stopActiveTransport();
    const canDowngrade = this.transportPolicy.adaptive
      && this.transportPolicy.downgrade.enabled
      && this.consecutiveErrors >= (this.transportPolicy.downgrade.consecutiveErrors || 1);
    const fallback = canDowngrade ? this.nextFallbackProtocol(this.protocol) : null;
    if (fallback) {
      const from = this.protocol;
      this.consecutiveErrors = 0;
      this.retryCount = 0;
      this.setStatus('degraded');
      this.emit('degraded', { from, to: fallback, reason });
      void this.connectProtocol(fallback, reason).catch(error => void this.handleConnectFailure(normalizeError(error)));
      return;
    }
    this.scheduleReconnect(reason, this.protocol);
  }

  private scheduleReconnect(reason: string, protocol: RealtimeProtocol | null = this.protocol): void {
    if (this.intentionalClose || !this.reconnectOptions.enabled) {
      this.setStatus('error');
      return;
    }
    if (this.retryCount >= this.reconnectOptions.maxRetries) {
      this.setStatus('error');
      return;
    }

    this.retryCount += 1;
    this.metrics.reconnectCount += 1;
    this.setStatus('reconnecting');
    const baseDelay = Math.min(
      this.reconnectOptions.minDelay * this.reconnectOptions.factor ** (this.retryCount - 1),
      this.reconnectOptions.maxDelay,
    );
    const delay = this.reconnectOptions.jitter ? Math.round(baseDelay * (0.8 + Math.random() * 0.4)) : baseDelay;
    this.emit('reconnecting', { retryCount: this.retryCount, delay });
    this.warn(`Realtime reconnect scheduled after ${delay}ms: ${reason}`);
    this.clearReconnectTimer();
    this.reconnectTimer = window.setTimeout(() => {
      if (protocol) {
        void this.connectProtocol(protocol, 'reconnect').catch(error => void this.handleConnectFailure(normalizeError(error)));
        return;
      }
      void this.connect();
    }, delay);
  }

  private startHeartbeat(): void {
    this.stopHeartbeat();
    if (!this.heartbeatOptions.enabled || !this.protocol) return;
    this.heartbeatTimer = window.setInterval(() => {
      void this.sendHeartbeat().catch(error => this.handleRuntimeError(normalizeError(error)));
    }, this.currentHeartbeatInterval());
  }

  private async sendHeartbeat(): Promise<void> {
    if (!this.protocol) return;
    const heartbeat = this.buildMessage({
      event: { domain: 'system', name: 'heartbeat.ping' },
      payload: { type: 'heartbeat', text: 'PING' },
      metadata: { heartbeat: true },
      ...(this.heartbeatOptions.payload?.() || {}),
    });
    this.metrics.heartbeatSentCount += 1;
    this.metrics.lastHeartbeatAt = Date.now();
    this.emit('heartbeat', {
      protocol: this.protocol,
      at: this.metrics.lastHeartbeatAt,
      direction: 'client',
      frame: 'ping',
      ok: true,
    });
    this.startHeartbeatTimeout();
    await this.sendHeartbeatPayload(heartbeat);
  }

  private async sendHeartbeatPayload(message: RealtimeMessage): Promise<void> {
    if (this.protocol === 'websocket') {
      if (!this.websocket || this.websocket.readyState !== WebSocket.OPEN) {
        throw new Error('WebSocket is not connected');
      }
      this.websocket.send(JSON.stringify({ type: 'ping' }));
      return;
    }

    // SSE/Polling do not have a bidirectional protocol frame. The active stream/poll request
    // is the liveness signal; sending HEART through inbound would be treated as business input.
    this.acknowledgeHeartbeat();
  }

  private startHeartbeatTimeout(): void {
    if (this.heartbeatTimeoutTimer) window.clearTimeout(this.heartbeatTimeoutTimer);
    this.heartbeatTimeoutTimer = window.setTimeout(() => {
      this.metrics.heartbeatMissCount += 1;
      if (this.metrics.heartbeatMissCount >= this.heartbeatOptions.missThreshold) {
        this.scheduleReconnectOrDowngrade('heartbeat-timeout');
      }
    }, this.heartbeatOptions.timeout);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) window.clearInterval(this.heartbeatTimer);
    if (this.heartbeatTimeoutTimer) window.clearTimeout(this.heartbeatTimeoutTimer);
    this.heartbeatTimer = null;
    this.heartbeatTimeoutTimer = null;
  }

  private startUpgradeProbe(): void {
    this.stopUpgradeProbe();
    if (!this.transportPolicy.adaptive || !this.transportPolicy.upgrade.enabled || !this.protocol) return;
    if (this.protocol === (this.transportPolicy.upgrade.target || 'websocket')) return;
    this.upgradeTimer = window.setInterval(() => {
      if (!this.protocol || this.status !== 'connected') return;
      const target = this.bestUpgradeTarget(this.protocol);
      if (!target) return;
      void this.tryUpgrade(target);
    }, this.transportPolicy.upgrade.interval);
  }

  private stopUpgradeProbe(): void {
    if (this.upgradeTimer) window.clearInterval(this.upgradeTimer);
    this.upgradeTimer = null;
  }

  private async tryUpgrade(target: RealtimeProtocol): Promise<void> {
    const from = this.protocol;
    this.warn(`Realtime probing upgrade from ${from} to ${target}`);
    this.stopActiveTransport();
    try {
      await this.connectProtocol(target, 'upgrade-probe');
    } catch (error) {
      this.warn(`Realtime upgrade failed: ${normalizeError(error).message}`);
      if (from) await this.connectProtocol(from, 'upgrade-rollback');
    }
  }

  private receiveMessage(message: RealtimeMessage): void {
    const normalizedMessage = this.normalizeIncomingMessage(message);
    if (this.consumeSystemMessage(normalizedMessage)) {
      return;
    }
    if (this.isDuplicate(normalizedMessage)) return;

    this.metrics.receivedCount += 1;
    this.metrics.lastMessageAt = Date.now();
    this.emit('message', normalizedMessage);
    messageEventAliases(normalizedMessage)
      .forEach(type => this.subscribers.get(type)?.forEach(handler => handler(normalizedMessage)));
    this.subscribers.get('*')?.forEach(handler => handler(normalizedMessage));
  }

  private normalizeIncomingMessage(message: RealtimeMessage): RealtimeMessage {
    const normalized = normalizeRealtimeEnvelope(message);
    const kind = normalizeMessageKind(normalized) || REALTIME_MESSAGE_KIND.CUSTOM;
    return { ...normalized, kind };
  }

  private consumeSystemMessage(message: RealtimeMessage): boolean {
    const kind = normalizeMessageKind(message);
    const isHeartbeat = isHeartbeatEnvelope(message);
    const isSystem = isHeartbeat || kind === REALTIME_MESSAGE_KIND.NOTICE || kind === REALTIME_MESSAGE_KIND.ERROR;
    if (!isSystem) return false;

    if (isHeartbeat) this.acknowledgeHeartbeat();
    if (message.event?.name === 'connection.connected') {
      const connectionId = message.source?.sessionId || (message.metadata?.connectionId as string | undefined);
      if (connectionId) this.sessionId = connectionId;
    }
    this.emit('system-message', { ...message, kind: kind || message.kind });
    return this.heartbeatOptions.suppressEvents;
  }

  private acknowledgeHeartbeat(): void {
    this.metrics.heartbeatMissCount = 0;
    if (this.heartbeatTimeoutTimer) window.clearTimeout(this.heartbeatTimeoutTimer);
    this.heartbeatTimeoutTimer = null;
    if (this.protocol) {
      this.emit('heartbeat', {
        protocol: this.protocol,
        at: Date.now(),
        direction: 'server',
        frame: 'pong',
        ok: true,
      });
    }
  }

  private isDuplicate(message: RealtimeMessage): boolean {
    if (!message.id) return false;
    const now = Date.now();
    const windowMs = 300000;
    if (this.seenMessages.has(message.id)) return true;
    this.seenMessages.set(message.id, now);
    if (this.seenMessages.size > 1000) {
      [...this.seenMessages.entries()]
        .filter(([, seenAt]) => now - seenAt > windowMs)
        .forEach(([id]) => this.seenMessages.delete(id));
    }
    return false;
  }

  private buildMessage(message: Partial<RealtimeMessage>): RealtimeMessage {
    return createRealtimeEnvelope(message, this.identity, this.sessionId, this.nextSequence());
  }

  private async postInbound(message: RealtimeMessage): Promise<RealtimeMessage | undefined> {
    const endpoint = this.protocol === 'sse' ? this.endpoints.inboundSse : this.endpoints.inboundPolling;
    const url = new URL(endpoint, window.location.origin);
    if (this.sessionId) url.searchParams.set('sessionId', this.sessionId);
    const response = await this.postJson<RealtimeMessage | undefined>(url.toString(), this.toWireMessage(message));
    if (response) this.receiveMessage(response);
    return response;
  }

  private toWireMessage(message: RealtimeMessage): RealtimeMessage {
    return message;
  }

  private nextSequence(): number {
    this.sequence += 1;
    return this.sequence;
  }

  private stopActiveTransport(): void {
    if (this.websocket) {
      this.websocket.close();
      this.websocket = null;
    }
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
    this.pollingActive = false;
    this.pollingAbortController?.abort();
    this.pollingAbortController = null;
  }

  private nextFallbackProtocol(current: RealtimeProtocol | null): RealtimeProtocol | null {
    const order = this.negotiatedOrder.length > 0 ? this.negotiatedOrder : this.transportPolicy.fallbackOrder;
    const index = current ? order.indexOf(current) : -1;
    return order.slice(index + 1).find(Boolean) || null;
  }

  private bestUpgradeTarget(current: RealtimeProtocol): RealtimeProtocol | null {
    return this.transportPolicy.fallbackOrder.find(protocol => PROTOCOL_RANK[protocol] < PROTOCOL_RANK[current]) || null;
  }

  private currentHeartbeatInterval(): number {
    if (typeof document !== 'undefined' && document.hidden) {
      return this.heartbeatOptions.hiddenInterval;
    }
    return this.heartbeatOptions.interval;
  }

  private setStatus(status: RealtimeStatus): void {
    this.status = status;
    this.metrics.status = status;
    this.emit('status', status);
  }

  private emit<T extends RealtimeEvent>(event: T, payload: RealtimeEventPayloadMap[T]): void {
    this.eventHandlers[event]?.forEach(handler => (handler as RealtimeEventHandler<T>)(payload));
  }

  private async fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
    const response = await fetch(url, {
      ...init,
      headers: {
        ...(await this.authHeaders()),
        ...(init?.headers || {}),
      },
    });
    return readResponse<T>(response);
  }

  private async postJson<T>(url: string, body: unknown): Promise<T> {
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        ...(await this.authHeaders()),
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });
    return readResponse<T>(response);
  }

  private async authHeaders(): Promise<Record<string, string>> {
    const headers = await resolveMaybe(this.auth?.headers);
    const token = await this.resolveToken();
    return {
      ...(headers || {}),
      ...(token ? { Authorization: this.auth?.tokenType === 'raw' ? token : `Bearer ${token}` } : {}),
      ...(this.identity.tenantId ? {
        'TENANT-ID': String(this.identity.tenantId),
        'X-Mango-Tenant-Id': String(this.identity.tenantId),
      } : {}),
      ...(this.identity.userId != null ? {
        'X-Mango-User-Id': String(this.identity.userId),
      } : {}),
      ...(this.identity.clientId ? {
        'X-Mango-Client-Id': this.identity.clientId,
      } : {}),
    };
  }

  private ticketUrl(endpoint: string, protocol: 'websocket' | 'sse'): URL {
    if (!this.connectionTicket) {
      throw new RealtimeConnectionError('Realtime connection ticket is missing', 'auth');
    }
    const url = this.transportUrl(endpoint, protocol);
    url.searchParams.set('rtTicket', this.connectionTicket);
    return url;
  }

  private transportUrl(endpoint: string, protocol: 'websocket' | 'sse'): URL {
    const url = new URL(endpoint, window.location.origin);
    if (protocol === 'websocket') {
      url.protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    }
    if (this.identity.clientId) {
      url.searchParams.set('clientId', this.identity.clientId);
    }
    return url;
  }

  private async resolveToken(): Promise<string | undefined> {
    const configured = await this.auth?.getToken?.();
    return configured || Session.getToken() || undefined;
  }

  private parseMessage(payload: string): RealtimeMessage {
    return decodeRealtimeEnvelope(payload);
  }

  private clearReconnectTimer(): void {
    if (this.reconnectTimer) window.clearTimeout(this.reconnectTimer);
    this.reconnectTimer = null;
  }

  private warn(message: string): void {
    if (this.debug) console.warn(message);
  }
}

export function createRealtimeClient(options: RealtimeOptions = {}): RealtimeClient {
  return new MangoRealtimeClient(options);
}

function normalizeReconnect(input?: RealtimeReconnectOptions | boolean): Required<RealtimeReconnectOptions> {
  if (input === false) return { ...DEFAULT_RECONNECT, enabled: false };
  if (input === true || input == null) return { ...DEFAULT_RECONNECT };
  return { ...DEFAULT_RECONNECT, ...input };
}

function normalizeHeartbeat(input?: RealtimeHeartbeatOptions | boolean, performanceMode?: string): Required<Omit<RealtimeHeartbeatOptions, 'payload'>> & Pick<RealtimeHeartbeatOptions, 'payload'> {
  if (input === false) return { ...DEFAULT_HEARTBEAT, enabled: false };
  const configured = input === true || input == null ? {} : input;
  const defaultMinInterval = performanceMode === 'aggressive' ? 500 : DEFAULT_HEARTBEAT.minInterval;
  const minInterval = configured.minInterval ?? defaultMinInterval;
  const interval = Math.max(configured.interval || DEFAULT_HEARTBEAT.interval, minInterval);
  return {
    ...DEFAULT_HEARTBEAT,
    ...configured,
    minInterval,
    interval,
    hiddenInterval: Math.max(configured.hiddenInterval || DEFAULT_HEARTBEAT.hiddenInterval, interval),
  };
}

function normalizeTransportPolicy(input?: RealtimeTransportPolicy): Required<RealtimeTransportPolicy> {
  return {
    adaptive: input?.adaptive ?? DEFAULT_POLICY.adaptive,
    fallbackOrder: input?.fallbackOrder || DEFAULT_POLICY.fallbackOrder,
    downgrade: {
      ...DEFAULT_POLICY.downgrade,
      ...(input?.downgrade || {}),
    },
    upgrade: {
      ...DEFAULT_POLICY.upgrade,
      ...(input?.upgrade || {}),
    },
  };
}

async function readResponse<T>(response: Response): Promise<T> {
  const text = await response.text();
  if (!response.ok) throw new RealtimeConnectionError(
    `HTTP ${response.status}: ${text || response.statusText}`,
    response.status === 401 ? 'auth' : response.status === 403 ? 'permission' : 'request',
    response.status,
  );
  if (!text) return undefined as T;
  return JSON.parse(text) as T;
}

async function resolveMaybe<T>(value?: T | (() => T | Promise<T>)): Promise<T | undefined> {
  if (!value) return undefined;
  return typeof value === 'function' ? (value as () => T | Promise<T>)() : value;
}

function normalizeError(error: unknown): Error {
  return error instanceof Error ? error : new Error(String(error));
}

function classifyRequestError(error: unknown, scope: RealtimeErrorScope): Error {
  const normalized = normalizeError(error);
  if (normalized instanceof RealtimeConnectionError) return normalized;
  return new RealtimeConnectionError(normalized.message, scope);
}

function isDowngradableError(error: Error): boolean {
  if (!(error instanceof RealtimeConnectionError)) return true;
  return error.scope === 'transport';
}

function uniqueProtocols(input: Array<RealtimeProtocol | null | undefined>): RealtimeProtocol[] {
  const result: RealtimeProtocol[] = [];
  input.forEach(protocol => {
    if (!protocol) return;
    if (!['websocket', 'sse', 'polling'].includes(protocol)) return;
    if (!result.includes(protocol)) result.push(protocol);
  });
  return result;
}

function createClientId(): string {
  return `browser-${Math.random().toString(16).slice(2, 10)}`;
}

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => window.setTimeout(resolve, ms));
}
