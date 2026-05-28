import { createRealtimeClient } from '@mango/common';
import type { RealtimeClient, RealtimeMessage, RealtimeOptions } from '@mango/common';
import type { NoticeSiteMessage } from '../types/notice';

export interface NoticeRealtimeEvent {
  messageId: string;
  title: string;
  bizType?: string;
  contentPreview?: string;
}

export type NoticeRealtimeHandler = (event: NoticeRealtimeEvent) => void | Promise<void>;

export interface NoticeRealtimeOptions {
  realtimeOptions?: RealtimeOptions;
}

export function requestDesktopPermission() {
  if (!('Notification' in window) || Notification.permission !== 'default') {
    return;
  }
  void Notification.requestPermission();
}

export function showDesktopNotice(message: NoticeSiteMessage, onClick: () => void) {
  if (!('Notification' in window) || Notification.permission !== 'granted') {
    return;
  }
  const notification = new Notification('您有新消息了', { body: message.title });
  notification.onclick = onClick;
}

export function playNoticeSound() {
  const audio = new Audio('data:audio/wav;base64,UklGRiQAAABXQVZFZm10IBAAAAABAAEAESsAACJWAAACABAAZGF0YQAAAAA=');
  void audio.play().catch(() => undefined);
}

export function speakNoticeText(text?: string) {
  const content = text?.trim();
  if (!content || !('speechSynthesis' in window) || typeof SpeechSynthesisUtterance === 'undefined') {
    return;
  }
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(content);
  utterance.lang = 'zh-CN';
  utterance.rate = 1;
  utterance.pitch = 1;
  window.speechSynthesis.speak(utterance);
}

export function createNoticeRealtime(handler: NoticeRealtimeHandler, options: NoticeRealtimeOptions = {}) {
  let client: RealtimeClient | undefined;
  const listener = (event: Event) => {
    const detail = (event as CustomEvent<NoticeRealtimeEvent>).detail;
    if (detail?.messageId) {
      notifyHandler(handler, detail);
    }
  };
  window.addEventListener('mango-notice-message', listener as EventListener);
  try {
    client = createRealtimeClient({ ...(options.realtimeOptions || {}), autoConnect: true });
    client.subscribe('notice', message => {
      const event = toNoticeRealtimeEvent(message);
      if (event?.messageId) {
        notifyHandler(handler, event);
      }
    });
  } catch {
    client = undefined;
  }
  return () => {
    window.removeEventListener('mango-notice-message', listener as EventListener);
    client?.disconnect('notice-bell-destroyed');
  };
}

function notifyHandler(handler: NoticeRealtimeHandler, event: NoticeRealtimeEvent) {
  void Promise.resolve(handler(event)).catch(() => undefined);
}

function toNoticeRealtimeEvent(message: RealtimeMessage): NoticeRealtimeEvent | undefined {
  const payload = normalizePayload(message.payload ?? message.content);
  const messageId = payload.messageId ?? payload.id;
  if (!messageId) {
    return undefined;
  }
  return {
    messageId: String(messageId),
    title: String(payload.title || ''),
    bizType: payload.bizType ? String(payload.bizType) : undefined,
    contentPreview: payload.contentPreview ? String(payload.contentPreview) : undefined,
  };
}

function normalizePayload(payload: unknown): Record<string, unknown> {
  if (!payload) {
    return {};
  }
  if (typeof payload === 'string') {
    try {
      const parsed = JSON.parse(payload);
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed as Record<string, unknown> : {};
    } catch {
      return {};
    }
  }
  if (typeof payload === 'object' && !Array.isArray(payload)) {
    const record = payload as Record<string, unknown>;
    if (typeof record.text === 'string') {
      return normalizePayload(record.text);
    }
    if (typeof record.content === 'string') {
      return normalizePayload(record.content);
    }
    return record;
  }
  return {};
}
