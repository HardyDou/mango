export type RealtimeProtocol = 'websocket' | 'sse' | 'polling';
export type RealtimeMode = 'auto' | RealtimeProtocol;
export type RealtimeStatus = 'idle' | 'connecting' | 'connected' | 'reconnecting' | 'degraded' | 'disconnected' | 'error';
export type RealtimePerformanceMode = 'normal' | 'aggressive';
export type RealtimeMessageKind = 'HEART' | 'PONG' | 'NOTICE' | 'CUSTOM' | 'ACK' | 'ERROR';

export interface RealtimeEventDescriptor {
  domain: string;
  name: string;
}

export interface RealtimeSource {
  platform?: string;
  clientId?: string;
  sessionId?: string;
}

export interface RealtimeContext {
  tenantId?: string;
  userId?: string | number | null;
  traceId?: string;
  requestId?: string;
}

export type RealtimeTargetType = 'USER' | 'CLIENT' | 'CONNECTION' | 'GROUP' | 'TENANT' | 'BROADCAST';

export interface RealtimeTarget {
  type: RealtimeTargetType;
  id?: string;
}

export interface RealtimeAck {
  required?: boolean;
  messageId?: string;
  accepted?: boolean;
}

export interface RealtimeStatusInfo {
  code: number;
  state: 'SUCCESS' | 'ERROR' | 'PENDING';
}

export interface RealtimePayload {
  type?: string;
  text?: string;
  content?: unknown;
  message?: string;
  [key: string]: unknown;
}

export const REALTIME_MESSAGE_KIND = {
  HEART: 'HEART',
  PONG: 'PONG',
  NOTICE: 'NOTICE',
  CUSTOM: 'CUSTOM',
  ACK: 'ACK',
  ERROR: 'ERROR',
} as const satisfies Record<RealtimeMessageKind, RealtimeMessageKind>;

export const REALTIME_SYSTEM_MESSAGE_KINDS: ReadonlySet<RealtimeMessageKind> = new Set([
  REALTIME_MESSAGE_KIND.HEART,
  REALTIME_MESSAGE_KIND.PONG,
  REALTIME_MESSAGE_KIND.NOTICE,
  REALTIME_MESSAGE_KIND.ACK,
  REALTIME_MESSAGE_KIND.ERROR,
]);

export interface RealtimeMessage {
  id?: string;
  version?: string;
  event?: RealtimeEventDescriptor;
  source?: RealtimeSource;
  context?: RealtimeContext;
  target?: RealtimeTarget;
  metadata?: Record<string, unknown>;
  payload?: RealtimePayload | unknown;
  ack?: RealtimeAck;
  status?: RealtimeStatusInfo;
  sequence?: number;
  timestamp?: string;
  stream?: Record<string, unknown>;
  /** @deprecated use event.name */
  kind?: RealtimeMessageKind;
  /** @deprecated use event.name */
  type?: string;
  /** @deprecated use payload */
  content?: unknown;
  /** @deprecated use context.tenantId */
  tenantId?: string;
  /** @deprecated use context.userId */
  userId?: string | number | null;
  /** @deprecated use source.sessionId */
  sessionId?: string;
  /** @deprecated use source.clientId */
  clientId?: string;
  /** @deprecated use metadata */
  headers?: Record<string, unknown>;
  /** @deprecated use timestamp */
  createdAt?: string;
  /** @deprecated use timestamp */
  receivedAt?: string;
}

export interface RealtimeEndpoints {
  negotiate?: string;
  websocket?: string;
  sse?: string;
  polling?: string;
  probeWebsocket?: string;
  probeSse?: string;
  probePolling?: string;
  inboundSse?: string;
  inboundPolling?: string;
}

export interface RealtimeIdentity {
  tenantId?: string;
  userId?: string | number | null;
  clientId?: string;
  sessionId?: string;
}

export interface RealtimeAuthOptions {
  getToken?: () => string | Promise<string | undefined> | undefined;
  tokenType?: 'Bearer' | 'raw';
  headers?: Record<string, string> | (() => Record<string, string> | Promise<Record<string, string>>);
  query?: Record<string, string> | (() => Record<string, string> | Promise<Record<string, string>>);
}

export interface RealtimeReconnectOptions {
  enabled?: boolean;
  maxRetries?: number;
  minDelay?: number;
  maxDelay?: number;
  factor?: number;
  jitter?: boolean;
}

export interface RealtimeHeartbeatOptions {
  enabled?: boolean;
  interval?: number;
  timeout?: number;
  minInterval?: number;
  hiddenInterval?: number;
  missThreshold?: number;
  suppressEvents?: boolean;
  payload?: () => Partial<RealtimeMessage>;
}

export interface RealtimeTransportPolicy {
  adaptive?: boolean;
  fallbackOrder?: RealtimeProtocol[];
  downgrade?: {
    enabled?: boolean;
    onConnectFailure?: boolean;
    onHeartbeatTimeout?: boolean;
    consecutiveErrors?: number;
  };
  upgrade?: {
    enabled?: boolean;
    interval?: number;
    onlyWhenIdle?: boolean;
    target?: RealtimeProtocol;
  };
}

export interface RealtimePollingOptions {
  maxSize?: number;
  timeoutMillis?: number;
  interval?: number;
}

export interface RealtimeMessageOptions {
  dedupe?: boolean;
  dedupeWindow?: number;
  maxDedupeIds?: number;
  offlineQueue?: boolean;
  offlineQueueLimit?: number;
  defaultHeaders?: Record<string, unknown>;
}

export interface RealtimePerformanceOptions {
  hiddenHeartbeatInterval?: number;
  throttleStatusEvents?: number;
  maxBufferedMessages?: number;
  sharedConnection?: boolean;
  idleMode?: 'keep-alive' | 'slow-heartbeat' | 'disconnect';
  idleAfter?: number;
}

export interface RealtimeOptions {
  mode?: RealtimeMode;
  endpoints?: RealtimeEndpoints;
  identity?: RealtimeIdentity;
  auth?: RealtimeAuthOptions;
  reconnect?: RealtimeReconnectOptions | boolean;
  heartbeat?: RealtimeHeartbeatOptions | boolean;
  transportPolicy?: RealtimeTransportPolicy;
  polling?: RealtimePollingOptions;
  message?: RealtimeMessageOptions;
  performance?: RealtimePerformanceOptions;
  performanceMode?: RealtimePerformanceMode;
  autoConnect?: boolean;
  debug?: boolean;
}

export interface RealtimeNegotiationTransport {
  type: RealtimeProtocol;
  enabled: boolean;
  endpoint?: string;
  available?: boolean;
  reason?: string;
  serverEnabled?: boolean;
  clientSupported?: boolean;
  contextReady?: boolean;
  handshakeRequired?: boolean;
  probeRequired?: boolean;
  probeEndpoint?: string;
}

export interface RealtimeNegotiationResponse {
  recommended?: RealtimeProtocol | null;
  transports?: RealtimeNegotiationTransport[];
  order?: RealtimeProtocol[];
  connectionTicket?: string;
  ticketExpiresAt?: number;
}

export interface RealtimeMetrics {
  protocol: RealtimeProtocol | null;
  status: RealtimeStatus;
  reconnectCount: number;
  receivedCount: number;
  sentCount: number;
  heartbeatSentCount: number;
  heartbeatMissCount: number;
  transportSwitchCount: number;
  lastConnectedAt?: number;
  lastMessageAt?: number;
  lastHeartbeatAt?: number;
}

export type RealtimeEvent =
  | 'status'
  | 'connected'
  | 'disconnected'
  | 'reconnecting'
  | 'reconnected'
  | 'message'
  | 'sent'
  | 'error'
  | 'transport-change'
  | 'heartbeat'
  | 'system-message'
  | 'degraded'
  | 'negotiated';

export type RealtimeEventPayloadMap = {
  status: RealtimeStatus;
  connected: { protocol: RealtimeProtocol };
  disconnected: { reason?: string };
  reconnecting: { retryCount: number; delay: number };
  reconnected: { protocol: RealtimeProtocol; retryCount: number };
  message: RealtimeMessage;
  sent: RealtimeMessage;
  error: Error;
  'transport-change': { from: RealtimeProtocol | null; to: RealtimeProtocol; reason: string };
  heartbeat: { protocol: RealtimeProtocol; at: number; direction: 'client' | 'server'; frame: 'ping' | 'pong'; ok: boolean };
  'system-message': RealtimeMessage;
  degraded: { from: RealtimeProtocol | null; to: RealtimeProtocol; reason: string };
  negotiated: RealtimeNegotiationResponse;
};

export type RealtimeEventHandler<T extends RealtimeEvent = RealtimeEvent> = (payload: RealtimeEventPayloadMap[T]) => void;
export type RealtimeMessageHandler = (message: RealtimeMessage) => void;
export type RealtimeUnsubscribe = () => void;

export interface RealtimeClient {
  connect(): Promise<void>;
  disconnect(reason?: string): void;
  reconnect(): Promise<void>;
  send(message: Partial<RealtimeMessage>): Promise<void>;
  subscribe(type: string, handler: RealtimeMessageHandler): RealtimeUnsubscribe;
  once(type: string, handler: RealtimeMessageHandler): RealtimeUnsubscribe;
  unsubscribe(type: string, handler: RealtimeMessageHandler): void;
  on<T extends RealtimeEvent>(event: T, handler: RealtimeEventHandler<T>): RealtimeUnsubscribe;
  off<T extends RealtimeEvent>(event: T, handler: RealtimeEventHandler<T>): void;
  getStatus(): RealtimeStatus;
  getProtocol(): RealtimeProtocol | null;
  getSessionId(): string | null;
  getClientId(): string | undefined;
  getMetrics(): RealtimeMetrics;
}
