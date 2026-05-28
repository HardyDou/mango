import { beforeEach, describe, expect, it, vi } from 'vitest';
import { createNoticeRealtime, playNoticeSound, requestDesktopPermission, showDesktopNotice, speakNoticeText } from '../noticeRealtime';
import type { NoticeSiteMessage } from '../../types/notice';

const realtimeMock = vi.hoisted(() => ({
  subscribeHandler: undefined as ((message: unknown) => void) | undefined,
  disconnect: vi.fn(),
  createRealtimeClient: vi.fn(() => ({
    subscribe: vi.fn((_type: string, handler: (message: unknown) => void) => {
      realtimeMock.subscribeHandler = handler;
      return vi.fn();
    }),
    disconnect: realtimeMock.disconnect,
  })),
}));

vi.mock('@mango/common', async importOriginal => ({
  ...(await importOriginal<typeof import('@mango/common')>()),
  createRealtimeClient: realtimeMock.createRealtimeClient,
}));

const message: NoticeSiteMessage = {
  id: '1001',
  title: '新的审批消息',
  content: '审批内容',
  userId: '1001',
  priority: 'NORMAL',
  readStatus: 'UNREAD',
};

describe('noticeRealtime', () => {
  beforeEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
    realtimeMock.subscribeHandler = undefined;
    realtimeMock.disconnect.mockClear();
    realtimeMock.createRealtimeClient.mockClear();
  });

  it('requestDesktopPermission 仅在默认权限时申请桌面通知权限', () => {
    const requestPermission = vi.fn();
    class FakeNotification {
      static permission = 'default';
      static requestPermission = requestPermission;
    }
    vi.stubGlobal('Notification', FakeNotification as unknown as typeof Notification);

    requestDesktopPermission();

    expect(requestPermission).toHaveBeenCalledTimes(1);
  });

  it('showDesktopNotice 在已授权时创建桌面通知并绑定点击回调', () => {
    const notifications: Array<FakeNotification> = [];
    class FakeNotification {
      static permission = 'granted';
      onclick?: () => void;

      constructor(public title: string, public options?: NotificationOptions) {
        notifications.push(this);
      }
    }
    vi.stubGlobal('Notification', FakeNotification as unknown as typeof Notification);
    const onClick = vi.fn();

    showDesktopNotice(message, onClick);
    notifications[0].onclick?.();

    expect(notifications[0].title).toBe('您有新消息了');
    expect(notifications[0].options?.body).toBe('新的审批消息');
    expect(onClick).toHaveBeenCalledTimes(1);
  });

  it('playNoticeSound 播放内置提示音并吞掉浏览器拒播异常', async () => {
    const play = vi.fn().mockRejectedValue(new Error('autoplay denied'));
    class FakeAudio {
      constructor(public src: string) {}
      play = play;
    }
    vi.stubGlobal('Audio', FakeAudio as unknown as typeof Audio);

    playNoticeSound();
    await Promise.resolve();

    expect(play).toHaveBeenCalledTimes(1);
  });

  it('speakNoticeText 使用浏览器语音 API 播报自定义内容', () => {
    const cancel = vi.fn();
    const speak = vi.fn();
    class FakeSpeechSynthesisUtterance {
      lang = '';
      rate = 0;
      pitch = 0;

      constructor(public text: string) {}
    }
    vi.stubGlobal('SpeechSynthesisUtterance', FakeSpeechSynthesisUtterance);
    Object.defineProperty(window, 'speechSynthesis', {
      configurable: true,
      value: { cancel, speak },
    });

    speakNoticeText(' 您有新的系统消息 ');

    expect(cancel).toHaveBeenCalledTimes(1);
    expect(speak).toHaveBeenCalledTimes(1);
    const utterance = speak.mock.calls[0][0] as FakeSpeechSynthesisUtterance;
    expect(utterance.text).toBe('您有新的系统消息');
    expect(utterance.lang).toBe('zh-CN');
  });

  it('createNoticeRealtime 订阅并可解除系统消息 realtime 事件', () => {
    const handler = vi.fn();
    const stop = createNoticeRealtime(handler);

    window.dispatchEvent(new CustomEvent('mango-notice-message', {
      detail: { messageId: '1001', title: '新的审批消息' },
    }));
    stop();
    window.dispatchEvent(new CustomEvent('mango-notice-message', {
      detail: { messageId: '1002', title: '解除后的消息' },
    }));

    expect(handler).toHaveBeenCalledTimes(1);
    expect(handler).toHaveBeenCalledWith({ messageId: '1001', title: '新的审批消息' });
  });

  it('createNoticeRealtime 将调用方 realtime 配置透传并解析文本 payload', async () => {
    const handler = vi.fn();
    const stop = createNoticeRealtime(handler, {
      realtimeOptions: {
        identity: { tenantId: '1', userId: 1 },
        mode: 'polling',
      },
    });

    realtimeMock.subscribeHandler?.({
      event: { domain: 'default', name: 'notice' },
      payload: { type: 'text', text: '{"messageId":"2001","title":"实时消息"}' },
    });
    await Promise.resolve();
    stop();

    expect(realtimeMock.createRealtimeClient).toHaveBeenCalledWith({
      identity: { tenantId: '1', userId: 1 },
      mode: 'polling',
      autoConnect: true,
    });
    expect(handler).toHaveBeenCalledWith({ messageId: '2001', title: '实时消息', bizType: undefined, contentPreview: undefined });
    expect(realtimeMock.disconnect).toHaveBeenCalledWith('notice-bell-destroyed');
  });
});
