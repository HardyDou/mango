import type { RealtimeIdentity, RealtimeMessage, RealtimeMessageKind } from './types';
import { REALTIME_MESSAGE_KIND, REALTIME_SYSTEM_MESSAGE_KINDS } from './types';

export const REALTIME_ENVELOPE_VERSION = '1.0';

export function createRealtimeEnvelope(
  input: Partial<RealtimeMessage>,
  identity: RealtimeIdentity,
  sessionId: string | null,
  sequence: number,
): RealtimeMessage {
  const event = input.event || legacyEvent(input.type) || { domain: 'default', name: 'message' };
  const payload = input.payload ?? legacyPayload(input.content);
  const metadata = {
    ...(input.headers || {}),
    ...(input.metadata || {}),
  };
  return {
    id: input.id || createMessageId(),
    version: input.version || REALTIME_ENVELOPE_VERSION,
    event,
    source: {
      platform: input.source?.platform || 'web',
      clientId: input.source?.clientId || input.clientId || identity.clientId,
      sessionId: input.source?.sessionId || input.sessionId || sessionId || identity.sessionId,
    },
    context: {
      tenantId: String(input.context?.tenantId ?? input.tenantId ?? identity.tenantId ?? 'default'),
      userId: input.context?.userId ?? input.userId ?? identity.userId ?? null,
      traceId: input.context?.traceId,
      requestId: input.context?.requestId,
    },
    target: input.target,
    metadata,
    payload,
    ack: input.ack,
    status: input.status,
    stream: input.stream,
    sequence: input.sequence || sequence,
    timestamp: input.timestamp || new Date().toISOString(),
  };
}

export function decodeRealtimeEnvelope(raw: unknown): RealtimeMessage {
  if (typeof raw === 'string') {
    try {
      return normalizeRealtimeEnvelope(JSON.parse(raw) as RealtimeMessage);
    } catch {
      return normalizeRealtimeEnvelope({ event: { domain: 'default', name: 'message' }, payload: { type: 'text', text: raw } });
    }
  }
  return normalizeRealtimeEnvelope(raw as RealtimeMessage);
}

export function normalizeRealtimeEnvelope(message: RealtimeMessage): RealtimeMessage {
  const event = message.event || legacyEvent(message.type) || { domain: 'default', name: 'message' };
  const payload = message.payload ?? legacyPayload(message.content);
  const metadata = {
    ...(message.headers || {}),
    ...(message.metadata || {}),
  };
  return {
    ...message,
    version: message.version || REALTIME_ENVELOPE_VERSION,
    event,
    source: {
      platform: message.source?.platform,
      clientId: message.source?.clientId ?? message.clientId,
      sessionId: message.source?.sessionId ?? message.sessionId,
    },
    context: {
      tenantId: message.context?.tenantId ?? message.tenantId,
      userId: message.context?.userId ?? message.userId,
      traceId: message.context?.traceId,
      requestId: message.context?.requestId,
    },
    target: message.target,
    metadata,
    payload,
    timestamp: message.timestamp || message.createdAt || message.receivedAt,
  };
}

export function messageEventName(message: RealtimeMessage): string {
  if (message.event?.domain && message.event.name) {
    return `${message.event.domain}.${message.event.name}`;
  }
  return message.event?.name || message.type || 'message';
}

export function messageEventAliases(message: RealtimeMessage): string[] {
  const aliases = new Set<string>();
  if (message.event?.domain && message.event.name) aliases.add(`${message.event.domain}.${message.event.name}`);
  if (message.event?.name) aliases.add(message.event.name);
  if (message.type) aliases.add(message.type);
  aliases.add('message');
  return [...aliases];
}

export function normalizeMessageKind(message: RealtimeMessage): RealtimeMessageKind | undefined {
  if (message.kind) return message.kind;
  const eventName = messageEventName(message);
  const bareEventName = message.event?.name || message.type || '';
  if (eventName === 'system.heartbeat.ping' || bareEventName === 'heartbeat.ping' || eventName === 'ping' || message.type === 'HEART') return REALTIME_MESSAGE_KIND.HEART;
  if (eventName === 'system.heartbeat.pong' || bareEventName === 'heartbeat.pong' || eventName === 'pong' || message.type === 'PONG') return REALTIME_MESSAGE_KIND.PONG;
  if (eventName === 'system.connection.connected' || bareEventName === 'connection.connected' || eventName === 'notice' || message.type === 'NOTICE') return REALTIME_MESSAGE_KIND.NOTICE;
  if (eventName.endsWith('.accepted') || eventName === 'ack' || message.type === 'ACK') return REALTIME_MESSAGE_KIND.ACK;
  if (eventName === 'error' || eventName.endsWith('.error') || message.type === 'ERROR' || message.status?.state === 'ERROR') return REALTIME_MESSAGE_KIND.ERROR;
  return undefined;
}

export function isSystemEnvelope(message: RealtimeMessage): boolean {
  const kind = normalizeMessageKind(message);
  return Boolean(kind && REALTIME_SYSTEM_MESSAGE_KINDS.has(kind));
}

export function isHeartbeatEnvelope(message: RealtimeMessage): boolean {
  const kind = normalizeMessageKind(message);
  return kind === REALTIME_MESSAGE_KIND.HEART
    || kind === REALTIME_MESSAGE_KIND.PONG
    || message.type === 'ping'
    || message.type === 'pong'
    || Boolean(message.headers?.heartbeat)
    || Boolean(message.metadata?.heartbeat);
}

export function getPayloadText(message: RealtimeMessage): string {
  const payload = message.payload;
  if (payload && typeof payload === 'object') {
    const record = payload as Record<string, unknown>;
    if (record.text != null) return String(record.text);
    if (record.content != null) return String(record.content);
    if (record.message != null) return String(record.message);
  }
  return message.content == null ? '' : String(message.content);
}

function createMessageId(): string {
  const random = Math.random().toString(36).slice(2, 12).toUpperCase();
  return `${Date.now().toString(36).toUpperCase()}${random}`.slice(0, 26);
}

function legacyEvent(type?: string): { domain: string; name: string } | undefined {
  if (!type) return undefined;
  if (type === 'connected') return { domain: 'system', name: 'connection.connected' };
  if (type === 'pong') return { domain: 'system', name: 'heartbeat.pong' };
  if (type === 'ping') return { domain: 'system', name: 'heartbeat.ping' };
  if (type === 'error') return { domain: 'system', name: 'message.error' };
  if (type === 'accepted' || type === 'ack') return { domain: 'system', name: 'message.accepted' };
  const [domain, ...rest] = type.split('.');
  if (rest.length > 0) return { domain, name: rest.join('.') };
  return { domain: 'default', name: type };
}

function legacyPayload(content: unknown): RealtimeMessage['payload'] {
  if (content && typeof content === 'object') return content;
  return { type: 'text', text: content == null ? '' : String(content) };
}
