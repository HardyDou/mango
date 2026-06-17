import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import { defineComponent, h } from 'vue';
import MangoDialog from '../index.vue';
import type { MangoDialogProps } from '../types';

const ElDialogStub = defineComponent({
  name: 'ElDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:modelValue', 'open', 'opened', 'close', 'closed'],
  setup(_props, { emit, slots, expose }) {
    expose({
      handleClose: () => {
        emit('update:modelValue', false);
        emit('close');
      },
    });

    return () => h('section', { class: 'el-dialog mango-dialog' }, [
      h('header', { class: 'el-dialog__header' }, slots.header?.()),
      h('main', { class: 'el-dialog__body' }, slots.default?.()),
      slots.footer ? h('footer', { class: 'el-dialog__footer' }, slots.footer()) : null,
    ]);
  },
});

const ElIconStub = defineComponent({
  name: 'ElIcon',
  setup(_props, { slots }) {
    return () => h('i', { class: 'el-icon' }, slots.default?.());
  },
});

const CloseStub = defineComponent({
  name: 'Close',
  setup() {
    return () => h('span', { class: 'mock-close-icon' });
  },
});

function mountDialog(props: Partial<MangoDialogProps> = {}) {
  return mount(MangoDialog, {
    props: {
      modelValue: true,
      title: 'Mango Dialog',
      ...props,
    },
    slots: {
      default: '<div class="content">Dialog content</div>',
      footer: '<button class="confirm-button">Confirm</button>',
    },
    global: {
      stubs: {
        ElDialog: ElDialogStub,
        ElIcon: ElIconStub,
        Close: CloseStub,
      },
    },
  });
}

describe('MangoDialog', () => {
  it('renders title, content and footer slot', () => {
    const wrapper = mountDialog();

    expect(wrapper.find('.mango-dialog__title').text()).toBe('Mango Dialog');
    expect(wrapper.find('.content').text()).toBe('Dialog content');
    expect(wrapper.find('.confirm-button').exists()).toBe(true);
  });

  it('supports close-only row when header is hidden', () => {
    const wrapper = mountDialog({ showHeader: false });

    expect(wrapper.find('.mango-dialog__title').exists()).toBe(false);
    expect(wrapper.find('.mango-dialog__header--close-only').exists()).toBe(true);
    expect(wrapper.find('.mango-dialog__close').exists()).toBe(true);
  });

  it('applies footer alignment class', () => {
    const wrapper = mountDialog({ footerAlign: 'center' });

    expect(wrapper.find('.mango-dialog__footer').classes()).toContain('mango-dialog__footer--center');
  });

  it('uses Element Plus close flow and emits model update', async () => {
    const wrapper = mountDialog();

    await wrapper.find('.mango-dialog__close').trigger('click');

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual([false]);
    expect(wrapper.emitted('close')).toBeTruthy();
  });
});
