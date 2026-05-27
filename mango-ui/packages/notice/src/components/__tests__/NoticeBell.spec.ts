import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import NoticeBell from '../NoticeBell.vue';
import type { NoticeRealtimeEvent } from '../../realtime/noticeRealtime';

const apiMock = vi.hoisted(() => ({
  getMyUnreadCount: vi.fn(),
  getMySiteMessages: vi.fn(),
  getMySiteMessageDetail: vi.fn(),
  markAllMySiteMessagesRead: vi.fn(),
  markMySiteMessageRead: vi.fn(),
  getChannelConfigs: vi.fn(),
}));

const realtimeMock = vi.hoisted(() => ({
  handler: undefined as ((event: NoticeRealtimeEvent) => void | Promise<void>) | undefined,
  stop: vi.fn(),
  createNoticeRealtime: vi.fn((handler: (event: NoticeRealtimeEvent) => void | Promise<void>) => {
    realtimeMock.handler = handler;
    return realtimeMock.stop;
  }),
  playNoticeSound: vi.fn(),
  requestDesktopPermission: vi.fn(),
  showDesktopNotice: vi.fn(),
  speakNoticeText: vi.fn(),
}));

const notificationMock = vi.hoisted(() => ({
  ElNotification: vi.fn(),
}));

vi.mock('../../api/notice', () => apiMock);
vi.mock('../../realtime/noticeRealtime', () => realtimeMock);
vi.mock('element-plus', () => notificationMock);
vi.mock('@mango/common/utils/iconConfig', () => ({
  iconMap: {
    Bell: { template: '<span data-test="bell-icon" />' },
  },
}));

const testMessage = {
  id: '1001',
  title: '测试系统消息',
  content: '系统消息内容',
  userId: '1001',
  priority: 'NORMAL',
  readStatus: 'UNREAD',
  bizType: 'SYSTEM_NOTICE',
  createTime: '2026-05-26 10:00:00',
};

function mountNoticeBell() {
  return mount(NoticeBell, {
    global: {
      stubs: {
        ElPopover: {
          template: '<div><slot name="reference" /><div data-test="popover-panel"><slot /></div></div>',
        },
        ElBadge: {
          props: ['value', 'hidden'],
          template: '<span class="notice-bell-test-badge"><slot /><span v-if="!hidden" data-test="badge-count">{{ value }}</span></span>',
        },
        ElIcon: {
          template: '<span><slot /></span>',
        },
        ElButton: {
          emits: ['click'],
          template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
        },
        ElEmpty: {
          template: '<div />',
        },
        NoticeDetailDialog: {
          props: ['modelValue', 'message'],
          template: '<div data-test="detail-dialog" :data-visible="String(modelValue)">{{ message?.content }}</div>',
        },
      },
    },
  });
}

describe('NoticeBell', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    realtimeMock.handler = undefined;
    apiMock.getMySiteMessages.mockResolvedValue({ list: [testMessage], total: 1, page: 1, size: 5 });
    apiMock.getMySiteMessageDetail.mockResolvedValue(testMessage);
    apiMock.markAllMySiteMessagesRead.mockResolvedValue(true);
    apiMock.markMySiteMessageRead.mockResolvedValue(true);
    apiMock.getChannelConfigs.mockResolvedValue({
      list: [{
        id: '270501',
        channelType: 'SITE',
        providerCode: 'INTERNAL',
        configName: '默认系统消息通道',
        configJson: JSON.stringify({
          soundEnabled: true,
          soundText: '您有新的系统消息，请及时查看',
          popupEnabled: true,
          desktopNotificationEnabled: true,
        }),
        enabled: true,
        priority: 0,
        weight: 100,
      }],
      total: 1,
      page: 1,
      size: 20,
    });
  });

  it('mounted 后拉取并显示未读数量', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 7 });

    const wrapper = mountNoticeBell();
    await flushPromises();

    expect(apiMock.getMyUnreadCount).toHaveBeenCalledTimes(1);
    expect(wrapper.get('[data-test="badge-count"]').text()).toBe('7');
    wrapper.unmount();
  });

  it('收到 realtime 消息后按接口结果刷新角标且不重复累加', async () => {
    apiMock.getMyUnreadCount
      .mockResolvedValueOnce({ count: 1 })
      .mockResolvedValueOnce({ count: 2 });

    const wrapper = mountNoticeBell();
    await flushPromises();

    await realtimeMock.handler?.({ messageId: '1002', title: '新的审批' });
    await flushPromises();

    expect(apiMock.getMyUnreadCount).toHaveBeenCalledTimes(2);
    expect(apiMock.getMySiteMessageDetail).toHaveBeenCalledWith('1002');
    expect(wrapper.get('[data-test="badge-count"]').text()).toBe('2');
    expect(realtimeMock.speakNoticeText).toHaveBeenCalledWith('您有新的系统消息，请及时查看');
    wrapper.unmount();
  });

  it('系统消息通道关闭声音提醒时不执行 TTS 播报', async () => {
    apiMock.getMyUnreadCount
      .mockResolvedValueOnce({ count: 1 })
      .mockResolvedValueOnce({ count: 2 });
    apiMock.getChannelConfigs.mockResolvedValue({
      list: [{
        id: '270501',
        channelType: 'SITE',
        providerCode: 'INTERNAL',
        configName: '默认系统消息通道',
        configJson: JSON.stringify({ soundEnabled: false, popupEnabled: true, desktopNotificationEnabled: true }),
        enabled: true,
        priority: 0,
        weight: 100,
      }],
      total: 1,
      page: 1,
      size: 20,
    });

    const wrapper = mountNoticeBell();
    await flushPromises();
    await realtimeMock.handler?.({ messageId: '1002', title: '新的审批' });
    await flushPromises();

    expect(realtimeMock.speakNoticeText).not.toHaveBeenCalled();
    expect(notificationMock.ElNotification).toHaveBeenCalledTimes(1);
    wrapper.unmount();
  });

  it('点击系统消息提醒后打开详情并标记已读', async () => {
    const detail = { ...testMessage, id: '1002', title: '新的审批', content: '审批详情' };
    apiMock.getMyUnreadCount
      .mockResolvedValueOnce({ count: 1 })
      .mockResolvedValueOnce({ count: 2 })
      .mockResolvedValueOnce({ count: 1 });
    apiMock.getMySiteMessageDetail.mockResolvedValue(detail);

    const wrapper = mountNoticeBell();
    await flushPromises();
    await realtimeMock.handler?.({ messageId: '1002', title: '新的审批' });
    await flushPromises();

    const notificationOptions = notificationMock.ElNotification.mock.calls[0][0];
    await notificationOptions.onClick();
    await flushPromises();

    expect(apiMock.markMySiteMessageRead).toHaveBeenCalledWith('1002');
    expect(wrapper.get('[data-test="detail-dialog"]').attributes('data-visible')).toBe('true');
    expect(wrapper.get('[data-test="detail-dialog"]').text()).toContain('审批详情');
    wrapper.unmount();
  });
});
