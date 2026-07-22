import { flushPromises, mount } from '@vue/test-utils';
import type { MangoWidgetRuntimeContext } from '@mango/grid-widgets';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MessageCenterWidget from '../MessageCenterWidget.vue';

const apiMock = vi.hoisted(() => ({
  getMySiteMessages: vi.fn(),
  getMyUnreadCount: vi.fn(),
  markAllMySiteMessagesRead: vi.fn(),
}));

const messageMock = vi.hoisted(() => ({
  error: vi.fn(),
  success: vi.fn(),
}));

vi.mock('../../../api/notice', () => apiMock);
vi.mock('element-plus', () => ({ ElMessage: messageMock }));

function mountWidget(navigate: NonNullable<MangoWidgetRuntimeContext['navigate']>, messageCenterPath?: string) {
  return mount(MessageCenterWidget, {
    props: {
      runtime: {
        pageCode: 'home',
        navigate,
      },
      ...(messageCenterPath ? { messageCenterPath } : {}),
    },
    global: {
      directives: {
        loading: () => undefined,
      },
    },
  });
}

async function clickViewAll(wrapper: ReturnType<typeof mountWidget>) {
  const button = wrapper.findAll('button').find((item) => item.text() === '查看全部');
  expect(button).toBeDefined();
  await button?.trigger('click');
  await flushPromises();
}

describe('MessageCenterWidget', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMock.getMyUnreadCount.mockResolvedValue({ count: 0 });
    apiMock.getMySiteMessages.mockResolvedValue({
      list: [],
      total: 0,
      page: 1,
      size: 1,
    });
    apiMock.markAllMySiteMessagesRead.mockResolvedValue(true);
  });

  it('默认跳转到已注册的消息中心站内信路由', async () => {
    const navigate = vi.fn<NonNullable<MangoWidgetRuntimeContext['navigate']>>().mockResolvedValue(undefined);
    const wrapper = mountWidget(navigate);
    await flushPromises();

    await clickViewAll(wrapper);

    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith({ path: '/message-center/site-message' });
    wrapper.unmount();
  });

  it('保留消费者覆盖消息中心路径的能力', async () => {
    const navigate = vi.fn<NonNullable<MangoWidgetRuntimeContext['navigate']>>().mockResolvedValue(undefined);
    const wrapper = mountWidget(navigate, '/custom/messages');
    await flushPromises();

    await clickViewAll(wrapper);

    expect(navigate).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledWith({ path: '/custom/messages' });
    wrapper.unmount();
  });
});
