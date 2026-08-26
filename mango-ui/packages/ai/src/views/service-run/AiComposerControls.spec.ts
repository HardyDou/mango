import type { AiServiceModelOption } from '@mango/ai-api';
import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import AiComposerControls from './AiComposerControls.vue';

const models: AiServiceModelOption[] = [
  {
    modelId: 'model-1',
    modelName: 'gpt-5.6-sol',
    displayName: 'GPT 5.6 Sol',
    providerCode: 'openai-compatible',
    providerDisplayName: 'OpenAI 协议',
    apiProtocol: 'RESPONSES',
    thinkingConfigurable: true,
    inputModalities: ['TEXT', 'IMAGE', 'FILE'],
    outputModalities: ['TEXT'],
  },
];

const selectStub = {
  props: ['modelValue', 'disabled'],
  emits: ['update:modelValue'],
  template:
    '<button type="button" class="select-stub" :disabled="disabled" @click="$emit(\'update:modelValue\', \'model-1\')"><slot /></button>',
};
const switchStub = {
  props: ['modelValue', 'disabled'],
  emits: ['update:modelValue'],
  template:
    '<button type="button" class="switch-stub" :disabled="disabled" @click="$emit(\'update:modelValue\', !modelValue)" />',
};

function mountControls() {
  return mount(AiComposerControls, {
    props: {
      models,
      modelValue: '',
      thinkingEnabled: false,
      thinkingTooltip: '切换思考模式',
    },
    global: {
      stubs: {
        ElSelect: selectStub,
        ElOption: { template: '<span><slot /></span>' },
        ElSwitch: switchStub,
        ElTooltip: { template: '<span><slot /></span>' },
      },
    },
  });
}

describe('AiComposerControls', () => {
  it('在输入工具栏派发模型和思考设置变更', async () => {
    const wrapper = mountControls();

    await wrapper.get('.select-stub').trigger('click');
    expect(wrapper.emitted('modelChange')).toEqual([['model-1']]);

    await wrapper.setProps({ modelValue: 'model-1' });
    await wrapper.get('.switch-stub').trigger('click');
    expect(wrapper.emitted('update:thinkingEnabled')).toEqual([[true]]);
  });

  it('模型选择始终保持可用，思考开关仅由模型能力控制', async () => {
    const wrapper = mountControls();

    expect(wrapper.attributes('data-state')).toBe('ai.service-run.next-turn-settings');
    expect(wrapper.get('.select-stub').attributes('disabled')).toBeUndefined();
    expect(wrapper.get('.switch-stub').attributes('disabled')).toBeDefined();

    await wrapper.setProps({ modelValue: 'model-1' });
    expect(wrapper.get('.switch-stub').attributes('disabled')).toBeUndefined();
  });
});
