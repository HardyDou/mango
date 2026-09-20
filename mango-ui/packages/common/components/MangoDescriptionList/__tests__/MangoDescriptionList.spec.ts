/* eslint-disable vue/one-component-per-file */
import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { describe, expect, it } from 'vitest';
import MangoDescriptionList from '../index.vue';

const ElDescriptionsStub = defineComponent({
  name: 'ElDescriptions',
  setup(_props, { slots }) {
    return () => h('dl', slots.default?.());
  },
});
const ElDescriptionsItemStub = defineComponent({
  name: 'ElDescriptionsItem',
  props: { label: { type: String, default: '' } },
  setup(props, { slots }) {
    return () => h('div', [h('dt', props.label), h('dd', slots.default?.())]);
  },
});
const RichTextStub = defineComponent({
  name: 'RichTextStub',
  props: { content: { type: String, default: '' } },
  setup(props) {
    return () => h('article', { 'data-rich-text': '' }, props.content);
  },
});

const global = {
  stubs: {
    ElDescriptions: ElDescriptionsStub,
    ElDescriptionsItem: ElDescriptionsItemStub,
    ElEmpty: defineComponent({ name: 'ElEmpty', setup: () => () => h('div', '空') }),
  },
};

describe('MangoDescriptionList', () => {
  it('支持分组、字段插槽和分组扩展内容', () => {
    const wrapper = mount(MangoDescriptionList, {
      props: {
        groups: [
          {
            key: 'base',
            header: { title: '基本信息', extraSlot: 'status' },
            items: [{ key: 'name', label: '申请人', value: '张三', slot: 'applicant' }],
          },
        ],
      },
      slots: {
        applicant: ({ displayValue }: { displayValue: string }) => h('strong', displayValue),
        'group-extra-status': () => h('span', '审核中'),
      },
      global,
    });

    expect(wrapper.text()).toContain('基本信息');
    expect(wrapper.text()).toContain('申请人：');
    expect(wrapper.find('dd strong').text()).toBe('张三');
    expect(wrapper.text()).toContain('审核中');
  });

  it('允许组合层替换富文本预览实现', () => {
    const wrapper = mount(MangoDescriptionList, {
      props: {
        items: [{ key: 'remark', label: '说明', value: '<p>内容</p>', componentType: 'rich-text-preview' }],
        richTextPreviewComponent: RichTextStub,
      },
      global,
    });

    expect(wrapper.find('[data-rich-text]').text()).toBe('<p>内容</p>');
  });

  it('拒绝同时传入 items 和 groups', () => {
    expect(() =>
      mount(MangoDescriptionList, {
        props: { items: [], groups: [] } as never,
        global,
      }),
    ).toThrow('items 与 groups 不能同时配置');
  });
});
