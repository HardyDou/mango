/* eslint-disable vue/one-component-per-file -- 测试内联 Element Plus stub 组件，无法拆分为独立文件。 */
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';

const errorMessage = vi.fn();

vi.mock('element-plus', () => ({
  ElMessage: { error: (...args: unknown[]) => errorMessage(...args), success: vi.fn() },
}));

vi.mock('@mango/common', async () => {
  const { defineComponent, h } = await import('vue');
  return {
    CodeEditor: defineComponent({
      name: 'CodeEditor',
      props: {
        modelValue: { type: String, default: '' },
        language: { type: String, default: '' },
        height: { type: String, default: '' },
        readonly: { type: Boolean, default: false },
        lineNumbers: { type: Boolean, default: false },
        matchBrackets: { type: Boolean, default: true },
        autoCloseBrackets: { type: Boolean, default: true },
      },
      setup(props: Record<string, unknown>) {
        return () => h('pre', { class: 'code-editor' }, props.modelValue as string);
      },
    }),
  };
});

import DemoCodeBlock from '../DemoCodeBlock.vue';

const ElButtonStub = defineComponent({
  name: 'ElButton',
  setup(_, { slots, attrs }) {
    return () => h('button', attrs, slots.default?.());
  },
});

function mountBlock(code: string, language?: string) {
  return mount(DemoCodeBlock, {
    props: { code, ...(language ? { language } : {}) },
    global: { stubs: { ElButton: ElButtonStub } },
  });
}

describe('MangoDevCodeBlock', () => {
  beforeEach(() => {
    errorMessage.mockReset();
  });

  it('renders the code through a read-only editor', () => {
    const wrapper = mountBlock('<div>示例</div>');
    const editor = wrapper.findComponent({ name: 'CodeEditor' });

    expect(editor.props('modelValue')).toBe('<div>示例</div>');
    expect(editor.props('readonly')).toBe(true);
    expect(editor.props('lineNumbers')).toBe(true);
    expect(editor.props('matchBrackets')).toBe(false);
    expect(editor.props('language')).toBe('html');
  });

  it('grows the editor height with the number of code lines and caps it', () => {
    const single = mountBlock('one');
    const many = mountBlock(Array.from({ length: 30 }, (_, index) => `line ${index}`).join('\n'));

    expect(single.findComponent({ name: 'CodeEditor' }).props('height')).toBe('128px');
    expect(many.findComponent({ name: 'CodeEditor' }).props('height')).toBe('420px');
  });

  it('copies the code and flips the button label to 已复制', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });

    const wrapper = mountBlock('const a = 1;');
    expect(wrapper.get('.copy-button').text()).toBe('复制');

    await wrapper.get('.copy-button').trigger('click');
    await wrapper.vm.$nextTick();

    expect(writeText).toHaveBeenCalledWith('const a = 1;');
    expect(wrapper.get('.copy-button').text()).toBe('已复制');
  });

  it('reports a copy failure without flipping the label', async () => {
    const writeText = vi.fn().mockRejectedValue(new Error('denied'));
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });

    const wrapper = mountBlock('const a = 1;');
    await wrapper.get('.copy-button').trigger('click');
    await wrapper.vm.$nextTick();

    expect(errorMessage).toHaveBeenCalledWith('复制失败');
    expect(wrapper.get('.copy-button').text()).toBe('复制');
  });
});
