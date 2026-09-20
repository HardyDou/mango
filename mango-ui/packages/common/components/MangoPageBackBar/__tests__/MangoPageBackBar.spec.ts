import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { routerKey, type Router } from 'vue-router';
import { describe, expect, it, vi } from 'vitest';
import MangoPageBackBar from '../index.vue';

const ElButtonStub = defineComponent({
  name: 'ElButton',
  inheritAttrs: false,
  setup(_props, { attrs, slots }) {
    return () => h('button', attrs, slots.default?.());
  },
});

describe('MangoPageBackBar', () => {
  it('无需 Router 即可发出返回和刷新事件', async () => {
    const wrapper = mount(MangoPageBackBar, {
      props: { title: '询单详情' },
      global: { stubs: { ElButton: ElButtonStub } },
    });

    expect(wrapper.text()).toContain('询单详情');
    const buttons = wrapper.findAll('button');
    await buttons[0].trigger('click');
    await buttons[1].trigger('click');

    expect(wrapper.emitted('back')).toHaveLength(1);
    expect(wrapper.emitted('refresh')).toHaveLength(1);
  });

  it('仅在明确开启导航时使用宿主 Router', async () => {
    const push = vi.fn().mockResolvedValue(undefined);
    const wrapper = mount(MangoPageBackBar, {
      props: { title: '订单详情', navigateOnBack: true, backTo: { name: 'orders' } },
      global: {
        provide: { [routerKey as symbol]: { push } as unknown as Router },
        stubs: { ElButton: ElButtonStub },
      },
    });

    await wrapper.find('button').trigger('click');
    expect(push).toHaveBeenCalledWith({ name: 'orders' });
  });
});
