import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import NoticeClientBell from '../../client/NoticeClientBell.vue';
import type { NoticeRealtimeEvent } from '../../realtime/noticeRealtime';

const apiMock = vi.hoisted(() => ({
  getMyUnreadCount: vi.fn(),
  getMyUnreadCategoryStats: vi.fn(),
  getMySiteMessages: vi.fn(),
  getMySiteMessageDetail: vi.fn(),
  markAllMySiteMessagesRead: vi.fn(),
  markMySiteMessageRead: vi.fn(),
  executeMySiteMessageAction: vi.fn(),
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
  ElMessage: { success: vi.fn() },
  ElMessageBox: { confirm: vi.fn(() => Promise.resolve()) },
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
          template:
            '<span class="notice-bell-test-badge"><slot /><span v-if="!hidden" data-test="badge-count">{{ value }}</span></span>',
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
          emits: ['action'],
          template:
            '<div data-test="detail-dialog" :data-visible="String(modelValue)">{{ message?.content }}<button v-if="message?.actions?.[0]" data-test="detail-action" @click="$emit(\'action\', message.actions[0])">action</button></div>',
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
    apiMock.getMyUnreadCategoryStats.mockResolvedValue({
      total: 1,
      categories: [
        { category: 'APPROVAL', count: 0 },
        { category: 'SYSTEM', count: 1 },
        { category: 'BUSINESS', count: 0 },
      ],
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
            template:
              '<span class="notice-bell-test-badge"><slot /><span v-if="!hidden" data-test="badge-count">{{ value }}</span></span>',
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
            emits: ['action'],
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
            template:
              '<span class="notice-bell-test-badge"><slot /><span v-if="!hidden" data-test="badge-count">{{ value }}</span></span>',
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
            emits: ['action'],
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
    expect(notificationOptions.title).toBe('SYSTEM_NOTICE');
    expect(notificationOptions.position).toBe('bottom-right');
    expect(notificationOptions.message.children[0].children[0].children[0].children).toBe('消息类型：');
    expect(notificationOptions.message.children[0].children[0].children[1].props.innerHTML).toBe('SYSTEM_NOTICE');
    expect(notificationOptions.message.children[0].children[1].children[0].children).toBe('消息内容：');
    expect(notificationOptions.message.children[0].children[1].children[1].props.innerHTML).toBe('系统消息内容');
    expect(notificationOptions.message.children[0].children[2].children[1].props.innerHTML).toBe('2026-05-26 10:00:00');
    expect(notificationOptions.message.children[1].children[0].children).toBe('点击查看');
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

  it('未读超过十条时显示分类统计并把分类筛选交给宿主', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 26 });
    apiMock.getMyUnreadCategoryStats.mockResolvedValue({
      total: 26,
      categories: [
        { category: 'APPROVAL', count: 15 },
        { category: 'SYSTEM', count: 10 },
        { category: 'BUSINESS', count: 1 },
      ],
    });
    const wrapper = mountNoticeBell();
    await flushPromises();

    expect(wrapper.find('.notice-bell__item').exists()).toBe(false);
    expect(wrapper.get('[data-test="notice-category-approval"]').text()).toBe('审批类消息（15条）');
    expect(wrapper.get('[data-test="notice-category-system"]').text()).toBe('系统通知（10条）');
    expect(wrapper.get('[data-test="notice-category-business"]').text()).toBe('业务通知（1条）');

    await wrapper.get('[data-test="notice-category-approval"]').trigger('click');
    expect(wrapper.emitted('view-all')?.[0]?.[0]).toEqual({ category: 'APPROVAL', unreadOnly: true });
    wrapper.unmount();
  });

  it('未读等于十条时仍展示单条消息列表', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 10 });
    apiMock.getMyUnreadCategoryStats.mockResolvedValue({
      total: 10,
      categories: [
        { category: 'APPROVAL', count: 10 },
        { category: 'SYSTEM', count: 0 },
        { category: 'BUSINESS', count: 0 },
      ],
    });
    const wrapper = mountNoticeBell();
    await flushPromises();

    expect(wrapper.find('.notice-bell__item').exists()).toBe(true);
    expect(wrapper.find('.notice-bell__categories').exists()).toBe(false);
    wrapper.unmount();
  });

  it('详情主操作通过 interaction 交给宿主导航并关闭弹框', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 1 });
    apiMock.getMySiteMessageDetail.mockResolvedValue({
      ...testMessage,
      actions: [
        {
          id: 'action-1',
          actionCode: 'OPEN_WORKFLOW',
          actionLabel: '去审批',
          interactionType: 'ROUTE',
          status: 'AVAILABLE',
          target: {
            targetType: 'ROUTE',
            targetKey: 'workflow:task:detail',
            params: { taskId: 'TASK-1' },
          },
        },
      ],
    });
    const wrapper = mountNoticeBell();
    await flushPromises();

    await wrapper.get('.notice-bell__item').trigger('click');
    await flushPromises();
    await wrapper.get('[data-test="detail-action"]').trigger('click');

    const interaction = wrapper.emitted('interaction')?.[0]?.[0] as {
      targetKey?: string;
      params?: Record<string, unknown>;
      onComplete?: (success: boolean) => void;
    };
    expect(interaction).toMatchObject({
      targetKey: 'workflow:task:detail',
      params: { taskId: 'TASK-1', messageId: '1001' },
    });
    interaction.onComplete?.(true);
    await wrapper.vm.$nextTick();
    expect(wrapper.get('[data-test="detail-dialog"]').attributes('data-visible')).toBe('false');
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

  it('消息列表保留安全 HTML 格式并移除危险内容', async () => {
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 1 });
    apiMock.getMySiteMessages.mockResolvedValue({
      list: [
        {
          ...testMessage,
          title: '<strong onclick="alert(1)">加急消息</strong><script>alert(1)</script>',
          content: '<span style="color:red">请查看</span><a href="javascript:alert(1)">详情</a>',
        },
      ],
      total: 1,
      page: 1,
      size: 10,
    });

    const wrapper = mountNoticeBell();
    await flushPromises();

    const titleHtml = wrapper.get('.notice-bell__title').element.innerHTML;
    const summaryHtml = wrapper.get('.notice-bell__summary').element.innerHTML;
    expect(titleHtml).toBe('<strong>加急消息</strong>');
    expect(summaryHtml).toBe('<span>请查看</span><a>详情</a>');
    expect(`${titleHtml}${summaryHtml}`).not.toContain('onclick');
    expect(`${titleHtml}${summaryHtml}`).not.toContain('javascript:');
    expect(`${titleHtml}${summaryHtml}`).not.toContain('<script');
    wrapper.unmount();
  });
});
