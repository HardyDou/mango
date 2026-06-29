import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import NoticeClientBell from '../../client/NoticeClientBell.vue';
import type { NoticeRealtimeEvent } from '../../realtime/noticeRealtime';

const apiMock = vi.hoisted(() => ({
  getMyUnreadCount: vi.fn(),
  getMySiteMessages: vi.fn(),
  getMySiteMessageDetail: vi.fn(),
  markAllMySiteMessagesRead: vi.fn(),
  markMySiteMessageRead: vi.fn(),
}));

const realtimeMock = vi.hoisted(() => ({
  handler: undefined as ((event: NoticeRealtimeEvent) => void | Promise<void>) | undefined,
  options: undefined as unknown,
  stop: vi.fn(),
  createNoticeRealtime: vi.fn((handler: (event: NoticeRealtimeEvent) => void | Promise<void>, options?: unknown) => {
    realtimeMock.handler = handler;
    realtimeMock.options = options;
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
  return mount(NoticeClientBell, {
    props: {
      runtimeConfig: {
        soundEnabled: true,
        soundText: '您有新的系统消息，请及时查看',
        reminderMode: 'VOICE',
        popupEnabled: true,
        desktopNotificationEnabled: true,
      },
      realtimeOptions: {
        identity: { tenantId: '1', userId: '1' },
      },
    },
    global: {
      stubs: {
        ElPopover: {
          emits: ['show'],
          mounted() {
            this.$emit('show');
          },
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
    window.location.hash = '';
    realtimeMock.handler = undefined;
    realtimeMock.options = undefined;
    apiMock.getMySiteMessages.mockResolvedValue({
      list: [{ ...testMessage, content: '系统消息内容摘要' }],
      total: 1,
      page: 1,
      size: 5,
    });
    apiMock.getMySiteMessageDetail.mockResolvedValue(testMessage);
    apiMock.markAllMySiteMessagesRead.mockResolvedValue(true);
    apiMock.markMySiteMessageRead.mockResolvedValue(true);
  });

  it('mounted 后拉取并显示未读数量', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 7 });

    const wrapper = mountNoticeBell();
    await flushPromises();

    expect(apiMock.getMyUnreadCount).toHaveBeenCalledTimes(1);
    expect(wrapper.get('[data-test="badge-count"]').text()).toBe('7');
    expect(realtimeMock.options).toEqual({ realtimeOptions: { identity: { tenantId: '1', userId: '1' } } });
    wrapper.unmount();
  });

  it('收到 realtime 消息后按事件未读数量刷新角标且不重复请求未读数接口', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 1 });

    const wrapper = mountNoticeBell();
    await flushPromises();

    await realtimeMock.handler?.({ messageId: '1002', title: '新的审批', unreadCount: 2 });
    await flushPromises();

    expect(apiMock.getMyUnreadCount).toHaveBeenCalledTimes(1);
    expect(apiMock.getMySiteMessageDetail).toHaveBeenCalledWith('1002');
    expect(wrapper.get('[data-test="badge-count"]').text()).toBe('2');
    expect(realtimeMock.speakNoticeText).toHaveBeenCalledWith('您有新的系统消息，请及时查看');
    wrapper.unmount();
  });

  it('收到仅包含未读数量的 realtime 事件时只更新角标', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 2 });

    const wrapper = mountNoticeBell();
    await flushPromises();

    await realtimeMock.handler?.({ title: '', unreadCount: 1 });
    await flushPromises();

    expect(apiMock.getMyUnreadCount).toHaveBeenCalledTimes(1);
    expect(apiMock.getMySiteMessageDetail).not.toHaveBeenCalled();
    expect(notificationMock.ElNotification).not.toHaveBeenCalled();
    expect(wrapper.get('[data-test="badge-count"]').text()).toBe('1');
    wrapper.unmount();
  });

  it('系统消息通道关闭声音提醒时不执行 TTS 播报', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 1 });
    const wrapper = mount(NoticeClientBell, {
      props: {
        runtimeConfig: { soundEnabled: false, popupEnabled: true, desktopNotificationEnabled: true },
      },
      global: {
        stubs: {
          ElPopover: {
            emits: ['show'],
            mounted() {
              this.$emit('show');
            },
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

    await flushPromises();
    await realtimeMock.handler?.({ messageId: '1002', title: '新的审批', unreadCount: 2 });
    await flushPromises();

    expect(apiMock.getMyUnreadCount).toHaveBeenCalledTimes(1);
    expect(realtimeMock.speakNoticeText).not.toHaveBeenCalled();
    expect(notificationMock.ElNotification).toHaveBeenCalledTimes(1);
    wrapper.unmount();
  });

  it('收到消息后按提醒设置显示右下弹窗并使用列表单条格式', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 1 });
    const wrapper = mount(NoticeClientBell, {
      props: {
        runtimeConfig: {
          voiceEnabled: true,
          reminderMode: 'VOICE',
          voiceText: '新的系统消息',
          popupEnabled: true,
          popupPlacement: 'bottom-right',
          desktopNotificationEnabled: false,
        },
      },
      global: {
        stubs: {
          ElPopover: {
            emits: ['show'],
            mounted() {
              this.$emit('show');
            },
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

    await flushPromises();
    await realtimeMock.handler?.({ messageId: '1002', title: '新的审批', unreadCount: 2 });
    await flushPromises();

    const notificationOptions = notificationMock.ElNotification.mock.calls[0][0];
    expect(apiMock.getMyUnreadCount).toHaveBeenCalledTimes(1);
    expect(realtimeMock.speakNoticeText).toHaveBeenCalledWith('新的系统消息');
    expect(realtimeMock.showDesktopNotice).not.toHaveBeenCalled();
    expect(notificationOptions.title).toBe('测试系统消息');
    expect(notificationOptions.position).toBe('bottom-right');
    expect(notificationOptions.message.children[0].children).toBe('系统消息内容');
    expect(notificationOptions.message.children[1].children).toBe('SYSTEM_NOTICE · 2026-05-26 10:00:00');
    wrapper.unmount();
  });

  it('点击系统消息提醒后打开详情并标记已读', async () => {
    const detail = { ...testMessage, id: '1002', title: '新的审批', content: '审批详情' };
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 1 });
    apiMock.getMySiteMessageDetail.mockResolvedValue(detail);

    const wrapper = mountNoticeBell();
    await flushPromises();
    await realtimeMock.handler?.({ messageId: '1002', title: '新的审批', unreadCount: 2 });
    await flushPromises();

    const notificationOptions = notificationMock.ElNotification.mock.calls[0][0];
    await notificationOptions.onClick();
    await flushPromises();

    expect(apiMock.markMySiteMessageRead).toHaveBeenCalledWith('1002');
    expect(apiMock.getMyUnreadCount).toHaveBeenCalledTimes(1);
    expect(wrapper.get('[data-test="detail-dialog"]').attributes('data-visible')).toBe('true');
    expect(wrapper.get('[data-test="detail-dialog"]').text()).toContain('审批详情');
    wrapper.unmount();
  });

  it('查看全部和接收设置只发事件，不绑定宿主路由', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 1 });
    const wrapper = mountNoticeBell();
    await flushPromises();

    await wrapper.get('[data-test="view-all-button"]').trigger('click');
    await wrapper.get('[data-test="settings-button"]').trigger('click');

    expect(wrapper.emitted('view-all')).toHaveLength(1);
    expect(wrapper.emitted('settings')).toHaveLength(1);
    expect(window.location.hash).toBe('');
    wrapper.unmount();
  });

  it('消息列表使用领域头像、标题和内容摘要展示', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 1 });
    const wrapper = mountNoticeBell();
    await flushPromises();

    expect(wrapper.get('.notice-bell__avatar').text()).toBe('S');
    expect(wrapper.get('.notice-bell__title').text()).toBe('测试系统消息');
    expect(wrapper.get('.notice-bell__summary').text()).toBe('系统消息内容摘要');
    wrapper.unmount();
  });
});
