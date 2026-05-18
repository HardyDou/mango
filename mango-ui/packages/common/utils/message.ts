import { ElMessage, type MessageParams, type MessageParamsWithType } from 'element-plus';

type MessageType = NonNullable<MessageParamsWithType['type']>;

const DEFAULT_WINDOW_MS = 1600;

interface MessageGateItem {
  key: string;
  message: string;
  type: MessageType;
  count: number;
  timer: ReturnType<typeof setTimeout>;
  close?: () => void;
}

let activeMessage: MessageGateItem | null = null;

interface ShowMessageOptions {
  merge?: boolean;
  windowMs?: number;
}

function normalizeMessage(message: string | MessageParams): string {
  if (typeof message === 'string') {
    return message;
  }
  return String(message.message || '');
}

function buildKey(type: MessageType, message: string): string {
  return `${type}:${message}`;
}

export function showMessage(
  type: MessageType,
  message: string | MessageParams,
  options: ShowMessageOptions = {}
): void {
  const normalizedMessage = normalizeMessage(message);
  const key = buildKey(type, normalizedMessage);
  const merge = options.merge ?? true;
  const windowMs = options.windowMs ?? DEFAULT_WINDOW_MS;

  if (activeMessage) {
    const isSameMessage = activeMessage.key === key;
    activeMessage.close?.();
    clearTimeout(activeMessage.timer);

    if (isSameMessage) {
      activeMessage.count += 1;
      const instance = ElMessage({
        ...(typeof message === 'string' ? {} : message),
        type,
        message: merge ? `${normalizedMessage}（${activeMessage.count}次）` : normalizedMessage,
      });
      activeMessage.close = instance.close;
      activeMessage.timer = setTimeout(() => {
        activeMessage = null;
      }, windowMs);
      return;
    }
  }

  const instance = ElMessage({
    ...(typeof message === 'string' ? {} : message),
    type,
    message: normalizedMessage,
  });
  activeMessage = {
    key,
    message: normalizedMessage,
    type,
    count: 1,
    close: instance.close,
    timer: setTimeout(() => {
      activeMessage = null;
    }, windowMs),
  };
}

export const mangoMessage = {
  success(message: string | MessageParams, options?: ShowMessageOptions): void {
    showMessage('success', message, options);
  },
  warning(message: string | MessageParams, options?: ShowMessageOptions): void {
    showMessage('warning', message, options);
  },
  info(message: string | MessageParams, options?: ShowMessageOptions): void {
    showMessage('info', message, options);
  },
  error(message: string | MessageParams, options?: ShowMessageOptions): void {
    showMessage('error', message, options);
  },
};
