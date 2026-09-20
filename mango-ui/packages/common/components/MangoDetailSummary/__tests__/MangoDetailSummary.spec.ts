import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { describe, expect, it } from 'vitest';
import MangoDetailSummary from '../index.vue';

describe('MangoDetailSummary', () => {
  it('展示标题、标签和摘要字段，并为空值提供占位', () => {
    const wrapper = mount(MangoDetailSummary, {
      props: {
        title: '履约保函',
        tags: [{ key: 'status', label: '处理中', type: 'warning' }],
        fields: [
          { key: 'amount', label: '金额', value: '100,000.00' },
          { key: 'bank', label: '银行', value: '' },
        ],
      },
      global: {
        stubs: {
          ElTag: defineComponent({
            name: 'ElTag',
            setup(_props, { slots }) {
              return () => h('span', { class: 'tag' }, slots.default?.());
            },
          }),
        },
      },
    });

    expect(wrapper.text()).toContain('履约保函');
    expect(wrapper.text()).toContain('处理中');
    expect(wrapper.text()).toContain('100,000.00');
    expect(wrapper.text()).toContain('银行-');
  });
});
