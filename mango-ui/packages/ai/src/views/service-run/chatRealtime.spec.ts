import type { AiServiceChatEvent } from '@mango/ai-api';
import type {
  RealtimeClient,
  RealtimeEvent,
  RealtimeEventHandler,
  RealtimeMessage,
  RealtimeMessageHandler,
} from '@mango/common/utils/realtime/types';
import { describe, expect, it, vi } from 'vitest';
import { waitForChatCompletion } from './chatRealtime';

describe('waitForChatCompletion', () => {
  it('按请求和服务过滤消息，忽略重复分片并在完成后清理订阅', async () => {
    const realtime = new TestRealtime();
    const events: AiServiceChatEvent[] = [];
    const controller = new AbortController();
    const completion = waitForChatCompletion(realtime, {
      requestId: 'request-1',
      serviceCode: 'assistant.general',
      signal: controller.signal,
      onEvent: (event) => events.push(event),
    });

    realtime.message(chatMessage('other-request', 1, false, { type: 'message', content: '忽略' }));
    realtime.message(chatMessage('request-1', 1, false, { type: 'message', content: '你' }));
    realtime.message(chatMessage('request-1', 1, false, { type: 'message', content: '重复' }));
    realtime.message(chatMessage('request-1', 2, true, doneEvent('request-1')));

    await expect(completion).resolves.toMatchObject({ type: 'done', requestId: 'request-1' });
    expect(events).toHaveLength(2);
    expect(events[0]).toEqual({ type: 'message', content: '你' });
    expect(realtime.subscriptionCount()).toBe(0);
  });

  it('收到模型错误后立即拒绝并清理订阅', async () => {
    const realtime = new TestRealtime();
    const completion = waitForChatCompletion(realtime, {
      requestId: 'request-1',
      serviceCode: 'assistant.general',
      signal: new AbortController().signal,
      onEvent: () => undefined,
    });

    realtime.message(chatMessage('request-1', 1, true, { type: 'error', message: '模型不可用' }));

    await expect(completion).rejects.toThrow('模型不可用');
    expect(realtime.subscriptionCount()).toBe(0);
  });

  it('Realtime 断线时立即失败，不等待固定超时', async () => {
    const realtime = new TestRealtime();
    const completion = waitForChatCompletion(realtime, {
      requestId: 'request-1',
      serviceCode: 'assistant.general',
      signal: new AbortController().signal,
      onEvent: () => undefined,
    });

    realtime.status('reconnecting');

    await expect(completion).rejects.toThrow('实时连接已中断');
    expect(realtime.subscriptionCount()).toBe(0);
  });

  it('主动取消和超时都释放订阅', async () => {
    vi.useFakeTimers();
    const realtime = new TestRealtime();
    const controller = new AbortController();
    const aborted = waitForChatCompletion(realtime, {
      requestId: 'request-1',
      serviceCode: 'assistant.general',
      signal: controller.signal,
      onEvent: () => undefined,
    });
    controller.abort();
    await expect(aborted).rejects.toMatchObject({ name: 'AbortError' });
    expect(realtime.subscriptionCount()).toBe(0);

    const timedOut = waitForChatCompletion(realtime, {
      requestId: 'request-2',
      serviceCode: 'assistant.general',
      signal: new AbortController().signal,
      onEvent: () => undefined,
      timeoutMs: 10,
    });
    const timedOutExpectation = expect(timedOut).rejects.toThrow('生成超时');
    await vi.advanceTimersByTimeAsync(10);
    await timedOutExpectation;
    expect(realtime.subscriptionCount()).toBe(0);
    vi.useRealTimers();
  });
});

class TestRealtime implements Pick<RealtimeClient, 'subscribe' | 'on'> {
  private messageHandler?: RealtimeMessageHandler;
  private statusHandler?: RealtimeEventHandler<'status'>;

  subscribe(type: string, handler: RealtimeMessageHandler) {
    expect(type).toBe('ai.service.chat');
    this.messageHandler = handler;
    return () => {
      if (this.messageHandler === handler) this.messageHandler = undefined;
    };
  }

  on<T extends RealtimeEvent>(event: T, handler: RealtimeEventHandler<T>) {
    expect(event).toBe('status');
    this.statusHandler = handler as RealtimeEventHandler<'status'>;
    return () => {
      if (this.statusHandler === handler) this.statusHandler = undefined;
    };
  }

  message(message: RealtimeMessage) {
    this.messageHandler?.(message);
  }

  status(status: 'reconnecting') {
    this.statusHandler?.(status);
  }

  subscriptionCount() {
    return Number(Boolean(this.messageHandler)) + Number(Boolean(this.statusHandler));
  }
}

function chatMessage(requestId: string, chunk: number, completed: boolean, event: AiServiceChatEvent): RealtimeMessage {
  return {
    event: { domain: 'ai', name: 'service.chat' },
    context: { requestId },
    metadata: { serviceCode: 'assistant.general', sessionId: 'session-1' },
    payload: { type: 'text', text: JSON.stringify(event) },
    stream: { id: requestId, chunk, completed },
  };
}

function doneEvent(requestId: string): Extract<AiServiceChatEvent, { type: 'done' }> {
  return {
    type: 'done',
    sessionId: 'session-1',
    requestId,
    modelId: '100',
    modelName: 'test-model',
    providerCode: 'test-provider',
    thinkingEnabled: false,
    contentParts: [{ type: 'RICH_TEXT', text: '你好' }],
  };
}
