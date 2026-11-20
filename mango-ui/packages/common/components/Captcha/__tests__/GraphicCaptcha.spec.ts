import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { defineComponent, h } from 'vue';

const generateGraphic = vi.fn();
const verifyCaptcha = vi.fn();

vi.mock('../../../api/captcha', () => ({
  CaptchaType: { GRAPHIC: 'GRAPHIC' },
  GraphicCaptchaType: { ALPHANUMERIC: 'ALPHANUMERIC', ARITHMETIC: 'ARITHMETIC' },
  generateGraphic: (...args: unknown[]) => generateGraphic(...args),
  verifyCaptcha: (...args: unknown[]) => verifyCaptcha(...args),
}));

const ElButtonStub = defineComponent({
  name: 'ElButton',
  setup(_, { slots, attrs }) {
    return () => h('button', attrs, slots.default?.());
  },
});

const ElInputStub = defineComponent({
  name: 'ElInput',
  props: { modelValue: { type: String, default: '' } },
  setup(props, { emit }) {
    return () =>
      h('input', {
        class: 'captcha-input-stub',
        value: props.modelValue,
        onInput: (event: Event) => emit('update:modelValue', (event.target as HTMLInputElement).value),
      });
  },
});

import GraphicCaptcha from '../GraphicCaptcha.vue';

function mountCaptcha() {
  return mount(GraphicCaptcha, {
    global: { stubs: { ElButton: ElButtonStub, ElInput: ElInputStub } },
  });
}

async function mountWithCode(code?: string) {
  generateGraphic.mockResolvedValue({ key: 'key-1', image: 'data:image/png;base64,AAA', graphicType: 'ALPHANUMERIC' });
  const wrapper = mountCaptcha();
  await flushPromises();
  if (code) {
    await wrapper.get('input').setValue(code);
  }
  return wrapper;
}

describe('GraphicCaptcha', () => {
  beforeEach(() => {
    generateGraphic.mockReset();
    verifyCaptcha.mockReset();
  });

  it('loads a graphic code on mount', async () => {
    const wrapper = await mountWithCode();

    expect(generateGraphic).toHaveBeenCalledTimes(1);
    expect(wrapper.find('img').exists()).toBe(true);
  });

  it('refreshes the code and emits refresh', async () => {
    const wrapper = await mountWithCode();
    generateGraphic.mockResolvedValue({ key: 'key-2', image: 'data:image/png;base64,BBB', graphicType: 'ALPHANUMERIC' });

    await wrapper.get('button').trigger('click');
    await flushPromises();

    expect(generateGraphic).toHaveBeenCalledTimes(2);
    expect(wrapper.emitted('refresh')?.length).toBeGreaterThanOrEqual(1);
  });

  it('rejects an empty input without calling the server', async () => {
    const wrapper = await mountWithCode();
    const result = await (wrapper.vm as unknown as { verify: () => Promise<boolean> }).verify();

    expect(result).toBe(false);
    expect(verifyCaptcha).not.toHaveBeenCalled();
    expect(wrapper.text()).toContain('请输入验证码');
  });

  it('emits success when the server accepts the code', async () => {
    verifyCaptcha.mockResolvedValue(true);
    const wrapper = await mountWithCode('abcd');

    const result = await (wrapper.vm as unknown as { verify: () => Promise<boolean> }).verify();

    expect(verifyCaptcha).toHaveBeenCalledWith({ key: 'key-1', type: 'GRAPHIC', code: 'abcd' });
    expect(result).toBe(true);
    expect(wrapper.emitted('success')?.[0]).toEqual(['key-1', 'abcd']);
  });

  it('reports a rejection and refreshes the code', async () => {
    verifyCaptcha.mockResolvedValue(false);
    const wrapper = await mountWithCode('wrong');

    const result = await (wrapper.vm as unknown as { verify: () => Promise<boolean> }).verify();
    await flushPromises();

    expect(result).toBe(false);
    // 校验失败会自动换一张图，换图时错误提示被清空，因此只断言行为不重复断言文案
    expect(generateGraphic).toHaveBeenCalledTimes(2);
    expect(wrapper.emitted('success')).toBeUndefined();
  });
});
