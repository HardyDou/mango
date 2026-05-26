import type { NoticeSiteMessage } from '../types/notice';

export interface NoticeRealtimeEvent {
  messageId: string;
  title: string;
  bizType?: string;
  contentPreview?: string;
}

export type NoticeRealtimeHandler = (event: NoticeRealtimeEvent) => void;

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

export function createNoticeRealtime(handler: NoticeRealtimeHandler) {
  const listener = (event: Event) => {
    const detail = (event as CustomEvent<NoticeRealtimeEvent>).detail;
    if (detail?.messageId) {
      handler(detail);
    }
  };
  window.addEventListener('mango-notice-message', listener as EventListener);
  return () => window.removeEventListener('mango-notice-message', listener as EventListener);
}
