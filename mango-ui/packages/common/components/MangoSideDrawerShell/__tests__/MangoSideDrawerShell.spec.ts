/* eslint-disable vue/one-component-per-file */
import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { describe, expect, it } from 'vitest';
import MangoSideDrawerShell from '../index.vue';
import type { MangoSideDrawerShellExpose } from '../types';

const ElButtonStub = defineComponent({
  name: 'ElButton',
  setup(_props, { attrs }) {
    return () => h('button', attrs);
  },
});
const ElDrawerStub = defineComponent({
  name: 'ElDrawer',
  props: { modelValue: Boolean },
  setup(_props, { slots }) {
    return () => h('aside', slots.default?.());
  },
});

describe('MangoSideDrawerShell', () => {
  it('默认展示固定触发按钮并支持点击打开', async () => {
    const wrapper = mount(MangoSideDrawerShell, {
      slots: { main: '<main>详情</main>', default: '<div>节点过程</div>' },
      global: { stubs: { ElButton: ElButtonStub, ElDrawer: ElDrawerStub } },
    });

    expect(wrapper.text()).toContain('详情');
    expect(wrapper.text()).toContain('节点过程');
    await wrapper.find('button').trigger('click');
    expect(wrapper.emitted('update:modelValue')).toEqual([[true]]);
  });

  it('向消费方暴露打开、关闭和切换方法', () => {
    const wrapper = mount(MangoSideDrawerShell, {
      props: { modelValue: false, showTrigger: false },
      global: { stubs: { ElButton: ElButtonStub, ElDrawer: ElDrawerStub } },
    });
    const exposed = wrapper.vm as unknown as MangoSideDrawerShellExpose;

    exposed.open();
    exposed.close();
    exposed.toggle();

    expect(wrapper.emitted('update:modelValue')).toEqual([[true], [false], [true]]);
  });
});
