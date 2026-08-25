import { parseAiServiceChatEvent, type AiServiceChatEvent } from '@mango/ai-api';
import { getPayloadText } from '@mango/common/utils/realtime/envelope';
import type { RealtimeClient, RealtimeMessage } from '@mango/common/utils/realtime/types';

const CHAT_EVENT_NAME = 'ai.service.chat';
const DEFAULT_COMPLETION_TIMEOUT_MS = 300_000;

export interface ChatCompletionOptions {
  requestId: string;
  serviceCode: string;
  signal: AbortSignal;
  onEvent: (event: AiServiceChatEvent) => void;
  timeoutMs?: number;
}

/** 等待一个受理请求的有序 Realtime 增量，终止时始终释放订阅和计时器。 */
export function waitForChatCompletion(
  realtime: Pick<RealtimeClient, 'subscribe' | 'on'>,
  options: ChatCompletionOptions,
): Promise<Extract<AiServiceChatEvent, { type: 'done' }>> {
  return new Promise((resolve, reject) => {
    let settled = false;
    let lastChunk = 0;
    let unsubscribeMessage: () => void = () => undefined;
    let unsubscribeStatus: () => void = () => undefined;
    const timeout = setTimeout(() => {
      finish(() => reject(new Error('AI 服务生成超时，请重试')));
    }, options.timeoutMs ?? DEFAULT_COMPLETION_TIMEOUT_MS);
    const abort = () => finish(() => reject(abortError()));
    const finish = (complete: () => void) => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      options.signal.removeEventListener('abort', abort);
      unsubscribeMessage();
      unsubscribeStatus();
      complete();
    };

    unsubscribeMessage = realtime.subscribe(CHAT_EVENT_NAME, (message: RealtimeMessage) => {
      if (message.context?.requestId !== options.requestId) return;
      try {
        const envelope = parseEnvelope(message, options.requestId, options.serviceCode);
        if (envelope.chunk <= lastChunk) return;
        lastChunk = envelope.chunk;
        if (envelope.event.type === 'error') {
          const message = envelope.event.message;
          finish(() => reject(new Error(message)));
          return;
        }
        options.onEvent(envelope.event);
        if (envelope.event.type === 'done') {
          const done = envelope.event;
          finish(() => resolve(done));
        }
      } catch (error) {
        finish(() => reject(error));
      }
    });
    unsubscribeStatus = realtime.on('status', (status) => {
      if (status === 'reconnecting' || status === 'disconnected' || status === 'error') {
        finish(() => reject(new Error('AI 实时连接已中断，请重试')));
      }
    });
    options.signal.addEventListener('abort', abort, { once: true });
    if (options.signal.aborted) abort();
  });
}

function parseEnvelope(message: RealtimeMessage, requestId: string, serviceCode: string) {
  if (message.metadata?.serviceCode !== serviceCode) {
    throw new Error('AI 实时事件与当前服务不匹配');
  }
  const stream = message.stream;
  if (!stream || stream.id !== requestId || !Number.isInteger(stream.chunk) || Number(stream.chunk) <= 0) {
    throw new Error('AI 实时事件缺少有效流标识');
  }
  const event = parseAiServiceChatEvent(getPayloadText(message));
  const terminal = event.type === 'done' || event.type === 'error';
  if (Boolean(stream.completed) !== terminal) {
    throw new Error('AI 实时事件完成状态不一致');
  }
  if (event.type === 'done' && event.requestId !== requestId) {
    throw new Error('AI 实时完成事件与当前请求不匹配');
  }
  return { event, chunk: Number(stream.chunk) };
}

function abortError() {
  const error = new Error('AI 服务生成已取消');
  error.name = 'AbortError';
  return error;
}
