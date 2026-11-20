import { describe, it, expect, vi, beforeEach } from 'vitest';
import { mount } from '@vue/test-utils';

import DemoDocLayout from '../DemoDocLayout.vue';

const tocItems = [
  { id: 'section-a', label: '示例 A' },
  { id: 'section-b', label: '示例 B' },
];

function mountLayout(props: Record<string, unknown> = {}) {
  return mount(DemoDocLayout, {
    props: { title: '组件文档', subtitle: '示例页', tocItems, ...props },
    slots: { default: '<p class="slot-content">插槽内容</p>' },
  });
}

describe('MangoDevDocLayout', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('renders title, subtitle, slot content and the article toc', () => {
    const wrapper = mountLayout();

    expect(wrapper.get('h1').text()).toBe('组件文档');
    expect(wrapper.get('header p').text()).toBe('示例页');
    expect(wrapper.get('.slot-content').text()).toBe('插槽内容');
    expect(wrapper.findAll('.article-toc button')).toHaveLength(2);
    expect(wrapper.get('.article-toc').attributes('aria-label')).toBe('页面导航');
  });

  it('marks the first toc item active on mount and toggles the boxed content wrapper', async () => {
    const wrapper = mountLayout({ contentBox: true });
    await wrapper.vm.$nextTick();

    const buttons = wrapper.findAll('.article-toc button');
    expect(buttons[0].classes()).toContain('active');
    expect(buttons[1].classes()).not.toContain('active');
    expect(wrapper.get('.doc-content').classes()).toContain('is-boxed');
  });

  it('scrolls to the section and activates it when a toc item is clicked', async () => {
    const target = document.createElement('div');
    target.id = 'section-b';
    target.scrollIntoView = vi.fn();
    document.body.appendChild(target);

    const wrapper = mountLayout();
    const buttons = wrapper.findAll('.article-toc button');

    await buttons[1].trigger('click');

    expect(target.scrollIntoView).toHaveBeenCalledTimes(1);
    expect(buttons[1].classes()).toContain('active');
  });

  it('observes the configured sections and disconnects on unmount', async () => {
    for (const item of tocItems) {
      const section = document.createElement('section');
      section.id = item.id;
      document.body.appendChild(section);
    }

    const observe = vi.spyOn(IntersectionObserver.prototype, 'observe');
    const disconnect = vi.spyOn(IntersectionObserver.prototype, 'disconnect');

    const wrapper = mountLayout();
    await wrapper.vm.$nextTick();

    expect(observe).toHaveBeenCalledTimes(2);

    wrapper.unmount();
    expect(disconnect).toHaveBeenCalled();
  });
});
