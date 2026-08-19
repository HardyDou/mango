import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import NoticeSecretInput from './NoticeSecretInput.vue';

const apiMock = vi.hoisted(() => ({
  getChannelSecret: vi.fn(),
}));
const messageMock = vi.hoisted(() => ({ error: vi.fn() }));

vi.mock('../../../api/notice', () => apiMock);
vi.mock('element-plus', () => ({ ElMessage: messageMock }));

function mountInput(props: Record<string, unknown> = {}) {
  return mount(NoticeSecretInput, {
    props: {
      modelValue: '',
      channelConfigId: '825',
      secretKey: 'password',
      configured: true,
      ...props,
    },
    global: {
      stubs: {
        ElInput: {
          inheritAttrs: false,
          props: ['modelValue', 'type', 'disabled', 'readonly', 'placeholder'],
          emits: ['input', 'focus', 'blur'],
          template:
            '<div><input v-bind="$attrs" :value="modelValue" :type="type" :readonly="readonly" @focus="$emit(\'focus\')" @blur="$emit(\'blur\')" @input="$emit(\'input\', $event.target.value)" /><slot name="suffix" /></div>',
        },
        ElButton: {
          inheritAttrs: false,
          props: ['disabled', 'loading', 'icon'],
          emits: ['click', 'mousedown'],
          template:
            '<button v-bind="$attrs" type="button" :disabled="disabled" @mousedown="$emit(\'mousedown\', $event)" @click="$emit(\'click\')" />',
        },
        ElTooltip: { template: '<span><slot /></span>' },
      },
    },
  });
}

describe('NoticeSecretInput', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    apiMock.getChannelSecret.mockResolvedValue({
      channelConfigId: '825',
      secretKey: 'password',
      value: 'real-smtp-password',
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('默认显示掩码并且只按当前字段请求明文，再次点击恢复掩码', async () => {
    const wrapper = mountInput();

    expect(wrapper.get('input').element.value).toBe('****');
    await wrapper.get('[data-action="notice.channel.secret.reveal"]').trigger('click');
    await flushPromises();

    expect(apiMock.getChannelSecret).toHaveBeenCalledWith('825', 'password', expect.anything());
    expect(wrapper.get('input').element.value).toBe('real-smtp-password');

    await wrapper.get('[data-action="notice.channel.secret.reveal"]').trigger('click');
    expect(wrapper.get('input').element.value).toBe('****');
    wrapper.unmount();
  });

  it('对话框清理信号会移除已查看的明文', async () => {
    const wrapper = mountInput({ resetToken: 0 });
    await wrapper.get('[data-action="notice.channel.secret.reveal"]').trigger('click');
    await flushPromises();
    expect(wrapper.get('input').element.value).toBe('real-smtp-password');

    await wrapper.setProps({ resetToken: 1 });

    expect(wrapper.get('input').element.value).toBe('****');
    wrapper.unmount();
  });

  it('切换渠道时丢弃尚未返回的旧渠道明文', async () => {
    let resolveRequest: ((value: { channelConfigId: string; secretKey: string; value: string }) => void) | undefined;
    apiMock.getChannelSecret.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveRequest = resolve;
        }),
    );
    const wrapper = mountInput();

    await wrapper.get('[data-action="notice.channel.secret.reveal"]').trigger('click');
    const signal = apiMock.getChannelSecret.mock.calls[0]?.[2] as AbortSignal;
    await wrapper.setProps({ channelConfigId: '900', resetToken: 1 });
    resolveRequest?.({ channelConfigId: '825', secretKey: 'password', value: 'stale-password' });
    await flushPromises();

    expect(signal.aborted).toBe(true);
    expect(wrapper.get('input').element.value).toBe('****');
    wrapper.unmount();
  });

  it('查看超时后清除明文并恢复掩码', async () => {
    vi.useFakeTimers();
    const wrapper = mountInput();
    await wrapper.get('[data-action="notice.channel.secret.reveal"]').trigger('click');
    await flushPromises();

    vi.advanceTimersByTime(60_000);
    await wrapper.vm.$nextTick();

    expect(wrapper.get('input').element.value).toBe('****');
    wrapper.unmount();
  });

  it('引用管理的 Secret 保持掩码且不请求明文', async () => {
    const wrapper = mountInput({ referenced: true });

    expect(wrapper.get('input').element.value).toBe('****');
    expect(wrapper.get('input').attributes('readonly')).toBeDefined();
    await wrapper.get('button').trigger('click');

    expect(apiMock.getChannelSecret).not.toHaveBeenCalled();
    wrapper.unmount();
  });

  it('查看失败时不泄露内容并保持掩码', async () => {
    apiMock.getChannelSecret.mockRejectedValue(new Error('denied'));
    const wrapper = mountInput();

    await wrapper.get('[data-action="notice.channel.secret.reveal"]').trigger('click');
    await flushPromises();

    expect(wrapper.get('input').element.value).toBe('****');
    expect(messageMock.error).toHaveBeenCalledWith('Secret 查看失败，已保持隐藏');
    wrapper.unmount();
  });

  it('替换 Secret 时只提交用户输入，不提交掩码', async () => {
    const wrapper = mountInput();
    await wrapper.get('input').trigger('focus');
    expect(wrapper.get('input').element.value).toBe('');

    await wrapper.get('input').setValue('new-password');

    expect(wrapper.emitted('update:modelValue')?.at(-1)).toEqual(['new-password']);
    expect(wrapper.emitted('update:modelValue')?.flat()).not.toContain('****');
    wrapper.unmount();
  });
});
