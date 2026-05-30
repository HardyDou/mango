import { createRealtimeClient } from '@mango/common';
import type { RealtimeClient, RealtimeMessage, RealtimeOptions } from '@mango/common';
import type { NoticeSiteMessage, NoticeSoundType } from '../types/notice';

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

const activeDesktopNotifications: Notification[] = [];

export function requestDesktopPermission() {
  if (!('Notification' in window) || Notification.permission !== 'default') {
    return;
  }
  void Notification.requestPermission();
}

export function showDesktopNotice(message: NoticeSiteMessage, onClick: () => void) {
  if (!('Notification' in window) || Notification.permission !== 'granted') {
    return false;
  }
  const notification = new Notification(message.title || '您有新消息了', {
    body: message.content || message.bizName || message.bizType || '',
    tag: message.id,
    requireInteraction: false,
    silent: false,
  });
  activeDesktopNotifications.push(notification);
  notification.onclick = () => {
    window.focus();
    onClick();
    notification.close();
  };
  notification.onclose = () => {
    const index = activeDesktopNotifications.indexOf(notification);
    if (index >= 0) {
      activeDesktopNotifications.splice(index, 1);
    }
  };
  window.setTimeout(() => notification.close(), 8000);
  return true;
}

export function playNoticeSound(soundType: NoticeSoundType = 'IM') {
  if (soundType === 'NONE') {
    return;
  }
  const audioContext = new AudioContext();
  const now = audioContext.currentTime;
  const steps = soundPattern(soundType);
  steps.forEach(step => {
    const oscillator = audioContext.createOscillator();
    const gain = audioContext.createGain();
    oscillator.type = 'sine';
    oscillator.frequency.setValueAtTime(step.frequency, now + step.start);
    gain.gain.setValueAtTime(0.0001, now + step.start);
    gain.gain.exponentialRampToValueAtTime(step.volume, now + step.start + 0.01);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + step.start + step.duration);
    oscillator.connect(gain);
    gain.connect(audioContext.destination);
    oscillator.start(now + step.start);
    oscillator.stop(now + step.start + step.duration + 0.02);
  });
  window.setTimeout(() => void audioContext.close().catch(() => undefined), 900);
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

function soundPattern(soundType: NoticeSoundType) {
  if (soundType === 'SOFT') {
    return [{ start: 0, duration: 0.18, frequency: 660, volume: 0.06 }];
  }
  if (soundType === 'DOUBLE') {
    return [
      { start: 0, duration: 0.12, frequency: 740, volume: 0.07 },
      { start: 0.16, duration: 0.12, frequency: 920, volume: 0.07 },
    ];
  }
  return [
    { start: 0, duration: 0.1, frequency: 880, volume: 0.08 },
    { start: 0.11, duration: 0.14, frequency: 1175, volume: 0.08 },
  ];
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
    client.subscribe('notice', (message: RealtimeMessage) => {
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
