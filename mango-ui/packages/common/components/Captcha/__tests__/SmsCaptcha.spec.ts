import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { defineComponent, h, nextTick } from 'vue';

const sendSms = vi.fn();
const verifyCaptcha = vi.fn();

vi.mock('../../../api/captcha', () => ({
  CaptchaType: { SMS: 'SMS' },
  sendSms: (...args: unknown[]) => sendSms(...args),
  verifyCaptcha: (...args: unknown[]) => verifyCaptcha(...args),
}));

const successMessage = vi.fn();
const errorMessage = vi.fn();

vi.mock('element-plus', () => ({
  ElMessage: {
    success: (...args: unknown[]) => successMessage(...args),
    error: (...args: unknown[]) => errorMessage(...args),
  },
}));

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: { modelValue: { type: String, default: '' } },
  setup(props, { emit }) {
    return () =>
      h('input', {
        class: 'el-input',
        value: props.modelValue,
        onInput: (event: Event) => emit('update:modelValue', (event.target as HTMLInputElement).value),
      });
  },
});

const ElButtonStub = defineComponent({
  name: 'ElButton',
  props: { disabled: { type: Boolean, default: false } },
  setup(props, { slots }) {
    return () => h('button', { class: 'el-button', disabled: props.disabled }, slots.default?.());
  },
});

async function mountCaptcha() {
  const SmsCaptcha = (await import('../SmsCaptcha.vue')).default;
  const wrapper = mount(SmsCaptcha, {
    global: {
      stubs: { ElInput: ElInputStub, ElButton: ElButtonStub },
    },
  });
  const firstInput = wrapper.find('input');
  await firstInput.setValue('13800000000');
  await nextTick();
  return wrapper;
}

describe('SmsCaptcha', () => {
  beforeEach(() => {
    sendSms.mockReset();
    verifyCaptcha.mockReset();
    successMessage.mockReset();
    errorMessage.mockReset();
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('stores the key and starts the countdown when the backend returns a captcha key', async () => {
    sendSms.mockResolvedValue('captcha-key-1');
    const wrapper = await mountCaptcha();

    await wrapper.find('button').trigger('click');
    await flushPromises();

    expect(sendSms).toHaveBeenCalledWith('13800000000');
    expect(successMessage).toHaveBeenCalledWith('验证码已发送');
    expect(wrapper.find('button').text()).toBe('60s');
    expect(wrapper.emitted('success')).toBeUndefined();
  });

  it('reports a failure and keeps no countdown when the backend answers with an empty key', async () => {
    sendSms.mockResolvedValue(undefined);
    const wrapper = await mountCaptcha();

    await wrapper.find('button').trigger('click');
    await flushPromises();

    expect(errorMessage).toHaveBeenCalledWith('验证码发送失败');
    expect(successMessage).not.toHaveBeenCalled();
    expect(wrapper.find('button').text()).toBe('发送验证码');
  });

  it('swallows the rejection of a failed send and shows no success message', async () => {
    sendSms.mockRejectedValue(new Error('2410 验证码发送失败'));
    const wrapper = await mountCaptcha();

    await wrapper.find('button').trigger('click');
    await flushPromises();

    expect(successMessage).not.toHaveBeenCalled();
    expect(wrapper.find('button').text()).toBe('发送验证码');
  });
});
