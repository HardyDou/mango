import { mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h, nextTick } from 'vue';
import MangoDialog from '../index.vue';
import type { MangoDialogProps } from '../types';

const ElDialogStub = defineComponent({
  name: 'ElDialog',
  inheritAttrs: false,
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
    width: {
      type: [String, Number],
      default: undefined,
    },
    modal: {
      type: Boolean,
      default: true,
    },
    modalPenetrable: {
      type: Boolean,
      default: false,
    },
    closeOnClickModal: {
      type: Boolean,
      default: true,
    },
    lockScroll: {
      type: Boolean,
      default: true,
    },
    zIndex: {
      type: Number,
      default: undefined,
    },
  },
  emits: ['update:modelValue', 'open', 'opened', 'close', 'closed'],
  setup(_props, { attrs, emit, slots, expose }) {
    expose({
      handleClose: () => {
        emit('update:modelValue', false);
        emit('close');
      },
    });

    return () =>
      h(
        'section',
        {
          ...attrs,
          class: ['el-dialog', 'mango-dialog', attrs.class],
          style: attrs.style,
        },
        [
          h('header', { class: 'el-dialog__header' }, slots.header?.()),
          h('main', { class: 'el-dialog__body' }, slots.default?.()),
          slots.footer ? h('footer', { class: 'el-dialog__footer' }, slots.footer()) : null,
        ],
      );
  },
});

const ElIconStub = defineComponent({
  name: 'ElIcon',
  setup(_props, { slots }) {
    return () => h('i', { class: 'el-icon' }, slots.default?.());
  },
});

const CloseStub = defineComponent({
  name: 'CloseIcon',
  setup() {
    return () => h('span', { class: 'mock-close-icon' });
  },
});

function mountDialog(props: Partial<MangoDialogProps> = {}, attrs: Record<string, unknown> = {}) {
  return mount(MangoDialog, {
    props: {
      modelValue: true,
      title: 'Mango Dialog',
      ...props,
    },
    attrs,
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

function createRect(left: number, top: number, width: number, height: number): DOMRect {
  return {
    bottom: top + height,
    height,
    left,
    right: left + width,
    top,
    width,
    x: left,
    y: top,
    toJSON: () => ({}),
  };
}

function mockDialogRect(wrapper: ReturnType<typeof mountDialog>, rect: DOMRect) {
  vi.spyOn(wrapper.get('.el-dialog').element, 'getBoundingClientRect').mockReturnValue(rect);
}

function dispatchPointer(type: 'pointermove' | 'pointerup', init: PointerEventInit) {
  document.dispatchEvent(
    new PointerEvent(type, {
      bubbles: true,
      isPrimary: true,
      pointerId: 1,
      ...init,
    }),
  );
}

beforeEach(() => {
  Object.defineProperty(document.documentElement, 'clientWidth', {
    configurable: true,
    value: 1280,
  });
  Object.defineProperty(document.documentElement, 'clientHeight', {
    configurable: true,
    value: 800,
  });
});

afterEach(() => {
  vi.restoreAllMocks();
  document.documentElement.style.cursor = '';
  document.documentElement.style.userSelect = '';
});

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

  it('keeps the modal mask and ignores mask clicks by default', () => {
    const wrapper = mountDialog();
    const dialog = wrapper.getComponent(ElDialogStub);

    expect(dialog.props('modal')).toBe(true);
    expect(dialog.props('closeOnClickModal')).toBe(false);
    expect(dialog.props('lockScroll')).toBe(true);
  });

  it('defaults draggable dialogs to a penetrable mask-free layer', () => {
    const wrapper = mountDialog({ draggable: true });
    const dialog = wrapper.getComponent(ElDialogStub);

    expect(dialog.props('modal')).toBe(false);
    expect(dialog.props('modalPenetrable')).toBe(true);
    expect(dialog.props('closeOnClickModal')).toBe(false);
    expect(dialog.props('lockScroll')).toBe(false);
  });

  it('allows draggable dialogs to retain a non-closing modal mask explicitly', () => {
    const wrapper = mountDialog({ draggable: true, modal: true });
    const dialog = wrapper.getComponent(ElDialogStub);

    expect(dialog.props('modal')).toBe(true);
    expect(dialog.props('modalPenetrable')).toBe(false);
    expect(dialog.props('closeOnClickModal')).toBe(false);
  });

  it('moves the whole dialog from the title area without viewport clamping', async () => {
    const wrapper = mountDialog({ draggable: true });
    mockDialogRect(wrapper, createRect(100, 80, 600, 400));

    await wrapper.get('.mango-dialog__header').trigger('pointerdown', {
      button: 0,
      clientX: 160,
      clientY: 110,
      isPrimary: true,
      pointerId: 1,
    });
    dispatchPointer('pointermove', { clientX: -140, clientY: -190 });
    await nextTick();

    const style = wrapper.get('.el-dialog').attributes('style');
    expect(style).toContain('left: -200px');
    expect(style).toContain('top: -220px');

    dispatchPointer('pointerup', { clientX: -140, clientY: -190 });
  });

  it('renders only four corner handles and resizes from the south-east corner', async () => {
    const wrapper = mountDialog({ resizable: true });
    mockDialogRect(wrapper, createRect(100, 80, 600, 400));

    expect(wrapper.findAll('.mango-dialog__resize-handle')).toHaveLength(4);

    await wrapper.get('.mango-dialog__resize-handle--south-east').trigger('pointerdown', {
      button: 0,
      clientX: 700,
      clientY: 480,
      isPrimary: true,
      pointerId: 1,
    });
    dispatchPointer('pointermove', { clientX: 820, clientY: 560 });
    await nextTick();

    const style = wrapper.get('.el-dialog').attributes('style');
    expect(style).toContain('width: 720px');
    expect(style).toContain('height: 480px');

    dispatchPointer('pointerup', { clientX: 820, clientY: 560 });
  });

  it('keeps the opposite corner fixed when resizing from the north-west corner', async () => {
    const wrapper = mountDialog({ resizable: true });
    mockDialogRect(wrapper, createRect(100, 80, 600, 400));

    await wrapper.get('.mango-dialog__resize-handle--north-west').trigger('pointerdown', {
      button: 0,
      clientX: 100,
      clientY: 80,
      isPrimary: true,
      pointerId: 1,
    });
    dispatchPointer('pointermove', { clientX: 200, clientY: 130 });
    await nextTick();

    const style = wrapper.get('.el-dialog').attributes('style');
    expect(style).toContain('left: 200px');
    expect(style).toContain('top: 130px');
    expect(style).toContain('width: 500px');
    expect(style).toContain('height: 350px');

    dispatchPointer('pointerup', { clientX: 200, clientY: 130 });
  });

  it('shrinks an interactively sized dialog when the viewport becomes smaller', async () => {
    const wrapper = mountDialog({ resizable: true });
    mockDialogRect(wrapper, createRect(100, 80, 900, 700));

    await wrapper.get('.mango-dialog__resize-handle--south-east').trigger('pointerdown', {
      button: 0,
      clientX: 1000,
      clientY: 780,
      isPrimary: true,
      pointerId: 1,
    });
    dispatchPointer('pointermove', { clientX: 1001, clientY: 781 });
    dispatchPointer('pointerup', { clientX: 1001, clientY: 781 });
    await nextTick();

    Object.defineProperty(document.documentElement, 'clientWidth', {
      configurable: true,
      value: 700,
    });
    Object.defineProperty(document.documentElement, 'clientHeight', {
      configurable: true,
      value: 500,
    });
    window.dispatchEvent(new Event('resize'));
    await nextTick();

    const style = wrapper.get('.el-dialog').attributes('style');
    expect(style).toContain('width: 676px');
    expect(style).toContain('height: 476px');
    expect(style).toContain('left: 100px');
    expect(style).toContain('top: 80px');
  });

  it('raises the clicked dialog above its previous layer', async () => {
    const wrapper = mountDialog({ draggable: true });
    const dialog = wrapper.getComponent(ElDialogStub);

    await wrapper.get('.mango-dialog__body').trigger('pointerdown');
    const firstZIndex = dialog.props('zIndex') as number;
    await wrapper.get('.mango-dialog__body').trigger('pointerdown');
    const secondZIndex = dialog.props('zIndex') as number;

    expect(firstZIndex).toBeTypeOf('number');
    expect(secondZIndex).toBeGreaterThan(firstZIndex);
  });

  it('uses a consumer z-index as the dynamic stacking baseline', async () => {
    const wrapper = mountDialog({ draggable: true, zIndex: 3200 });
    const dialog = wrapper.getComponent(ElDialogStub);

    await wrapper.get('.mango-dialog__body').trigger('pointerdown');
    const firstZIndex = dialog.props('zIndex') as number;
    await wrapper.get('.mango-dialog__body').trigger('pointerdown');
    const secondZIndex = dialog.props('zIndex') as number;

    expect(firstZIndex).toBeGreaterThanOrEqual(3200);
    expect(secondZIndex).toBeGreaterThan(firstZIndex);
  });

  it('preserves consumer class and style after switching to interactive layout', async () => {
    const wrapper = mountDialog({ draggable: true }, { class: 'consumer-dialog', style: 'color: red' });
    mockDialogRect(wrapper, createRect(100, 80, 600, 400));

    await wrapper.get('.mango-dialog__header').trigger('pointerdown', {
      button: 0,
      clientX: 160,
      clientY: 110,
      isPrimary: true,
      pointerId: 1,
    });
    dispatchPointer('pointermove', { clientX: 200, clientY: 150 });
    dispatchPointer('pointerup', { clientX: 200, clientY: 150 });
    await nextTick();

    const dialog = wrapper.get('.el-dialog');
    expect(dialog.classes()).toContain('consumer-dialog');
    expect(dialog.attributes('style')).toContain('color: red');
    expect(dialog.attributes('style')).toContain('position: fixed');
  });

  it('cleans up document interaction state when unmounted during a drag', async () => {
    const wrapper = mountDialog({ draggable: true });
    mockDialogRect(wrapper, createRect(100, 80, 600, 400));

    await wrapper.get('.mango-dialog__header').trigger('pointerdown', {
      button: 0,
      clientX: 160,
      clientY: 110,
      isPrimary: true,
      pointerId: 1,
    });
    expect(document.documentElement.style.userSelect).toBe('none');

    wrapper.unmount();
    expect(document.documentElement.style.cursor).toBe('');
    expect(document.documentElement.style.userSelect).toBe('');

    dispatchPointer('pointermove', { clientX: 240, clientY: 180 });
  });

  it('restores adaptive sizing after the dialog closes', async () => {
    const wrapper = mountDialog({ draggable: true });
    mockDialogRect(wrapper, createRect(100, 80, 600, 400));

    await wrapper.get('.mango-dialog__header').trigger('pointerdown', {
      button: 0,
      clientX: 160,
      clientY: 110,
      isPrimary: true,
      pointerId: 1,
    });
    dispatchPointer('pointermove', { clientX: 200, clientY: 150 });
    dispatchPointer('pointerup', { clientX: 200, clientY: 150 });
    await nextTick();
    expect(wrapper.get('.el-dialog').attributes('style')).toContain('position: fixed');

    wrapper.getComponent(ElDialogStub).vm.$emit('closed');
    await nextTick();
    expect(wrapper.get('.el-dialog').attributes('style') ?? '').not.toContain('position: fixed');
  });
});
