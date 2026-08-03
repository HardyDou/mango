import { mount } from '@vue/test-utils';
import { ElDialog } from 'element-plus';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { defineComponent, h, nextTick, onMounted, ref } from 'vue';
import MangoDialog from '../index.vue';
import type { MangoDialogExpose, MangoDialogProps } from '../types';

const ElDialogStub = defineComponent({
  name: 'ElDialog',
  inheritAttrs: false,
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
    title: {
      type: String,
      default: '',
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
  setup(props, { attrs, emit, slots, expose }) {
    const titleId = 'el-dialog-title-test';

    expose({
      handleClose: () => {
        emit('update:modelValue', false);
        emit('close');
      },
    });

    onMounted(() => {
      if (props.modelValue) emit('open');
    });

    return () =>
      h(
        'section',
        {
          ...attrs,
          'aria-label': props.title || undefined,
          'aria-labelledby': props.title ? undefined : titleId,
          class: ['el-dialog', 'mango-dialog', attrs.class],
          role: 'dialog',
          style: attrs.style,
        },
        [
          h('header', { class: 'el-dialog__header' }, slots.header?.({ titleId })),
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

const dialogStubs = {
  ElDialog: ElDialogStub,
  ElIcon: ElIconStub,
  Close: CloseStub,
};

function mountDialog(
  props: Partial<MangoDialogProps> = {},
  attrs: Record<string, unknown> = {},
  slots: Record<string, string> = {},
) {
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
      ...slots,
    },
    global: {
      stubs: dialogStubs,
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

  it('uses the title prop as the dialog accessible name', () => {
    const wrapper = mountDialog({ title: '批量识别企业资质' });

    expect(wrapper.get('[role="dialog"]').attributes('aria-label')).toBe('批量识别企业资质');
  });

  it('keeps the title prop as the accessible name when the title slot overrides the visible title', () => {
    const wrapper = mountDialog({ title: '批量识别企业资质' }, {}, { title: '<span>自定义视觉标题</span>' });

    expect(wrapper.get('.mango-dialog__title').text()).toBe('自定义视觉标题');
    expect(wrapper.get('[role="dialog"]').attributes('aria-label')).toBe('批量识别企业资质');
  });

  it('labels the dialog from the visible title when only the title slot is provided', () => {
    const wrapper = mountDialog({ title: '' }, {}, { title: '<span>批量识别企业资质</span>' });
    const dialog = wrapper.get('[role="dialog"]');
    const title = wrapper.get('.mango-dialog__title');

    expect(dialog.attributes('aria-label')).toBeUndefined();
    expect(dialog.attributes('aria-labelledby')).toBe(title.attributes('id'));
    expect(title.text()).toBe('批量识别企业资质');
  });

  it('retains the title prop as the accessible name when the visual header is hidden', () => {
    const wrapper = mountDialog({ showHeader: false, title: '批量识别企业资质' });

    expect(wrapper.find('.mango-dialog__title').exists()).toBe(false);
    expect(wrapper.get('[role="dialog"]').attributes('aria-label')).toBe('批量识别企业资质');
  });

  it('integrates with Element Plus accessible naming for a custom visible title', async () => {
    const wrapper = mount(MangoDialog, {
      props: {
        modelValue: true,
        title: '批量识别企业资质',
      },
      attrs: {
        teleported: false,
      },
      slots: {
        title: '<span>自定义视觉标题</span>',
      },
      global: {
        components: {
          ElDialog,
        },
        stubs: {
          ElIcon: ElIconStub,
        },
      },
    });

    await nextTick();

    const dialog = wrapper.get('[role="dialog"]');
    expect(wrapper.get('.mango-dialog__title').text()).toBe('自定义视觉标题');
    expect(dialog.attributes('aria-label')).toBe('批量识别企业资质');
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
    const wrapper = mountDialog({ draggable: true, modelValue: false });
    const dialog = wrapper.getComponent(ElDialogStub);

    await wrapper.setProps({ modelValue: true });
    await wrapper.get('.mango-dialog__body').trigger('pointerdown');
    const firstZIndex = dialog.props('zIndex') as number;
    await wrapper.get('.mango-dialog__body').trigger('pointerdown');
    const secondZIndex = dialog.props('zIndex') as number;
    await wrapper.get('.confirm-button').trigger('pointerdown');
    const footerZIndex = dialog.props('zIndex') as number;

    expect(firstZIndex).toBeTypeOf('number');
    expect(secondZIndex).toBeGreaterThan(firstZIndex);
    expect(footerZIndex).toBeGreaterThan(secondZIndex);
  });

  it('raises from interactive content without blocking its action', async () => {
    const handleClick = vi.fn();
    const wrapper = mount(MangoDialog, {
      props: {
        modelValue: true,
      },
      slots: {
        default: () => h('button', { class: 'dialog-action', onClick: handleClick }, 'Action'),
      },
      global: {
        stubs: dialogStubs,
      },
    });
    await nextTick();
    const dialog = wrapper.getComponent(ElDialogStub);
    const initialZIndex = dialog.props('zIndex') as number;
    const action = wrapper.get('.dialog-action');

    await action.trigger('pointerdown');
    await action.trigger('click');

    expect(dialog.props('zIndex')).toBeGreaterThan(initialZIndex);
    expect(handleClick).toHaveBeenCalledOnce();
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

  it('programmatically switches the highest layer between open dialog instances', async () => {
    const firstDialogRef = ref<MangoDialogExpose | null>(null);
    const secondDialogRef = ref<MangoDialogExpose | null>(null);
    // A local host is required to exercise multiple component refs in one test.
    // eslint-disable-next-line vue/one-component-per-file
    const Host = defineComponent({
      setup() {
        return () =>
          h('div', [
            h(MangoDialog, {
              ref: firstDialogRef,
              modelValue: true,
              title: 'First dialog',
            }),
            h(MangoDialog, {
              ref: secondDialogRef,
              modelValue: true,
              title: 'Second dialog',
            }),
          ]);
      },
    });
    const wrapper = mount(Host, {
      global: {
        stubs: dialogStubs,
      },
    });
    await nextTick();

    const dialogs = wrapper.findAllComponents(MangoDialog);
    const firstDialog = dialogs[0].getComponent(ElDialogStub);
    const secondDialog = dialogs[1].getComponent(ElDialogStub);
    const initialFirstZIndex = firstDialog.props('zIndex') as number;
    const initialSecondZIndex = secondDialog.props('zIndex') as number;
    expect(initialFirstZIndex).not.toBe(initialSecondZIndex);

    firstDialogRef.value?.bringToFront();
    await nextTick();
    expect(firstDialog.props('zIndex')).toBeGreaterThan(secondDialog.props('zIndex') as number);

    secondDialogRef.value?.bringToFront();
    await nextTick();
    expect(secondDialog.props('zIndex')).toBeGreaterThan(firstDialog.props('zIndex') as number);

    wrapper.unmount();
  });

  it('safely ignores programmatic stacking while closed, closing, or destroyed', async () => {
    const dialogRef = ref<MangoDialogExpose | null>(null);
    const visible = ref(false);
    const rendered = ref(true);
    // A local host is required to exercise ref safety across unmounting.
    // eslint-disable-next-line vue/one-component-per-file
    const Host = defineComponent({
      setup() {
        return () =>
          rendered.value
            ? h(MangoDialog, {
                ref: dialogRef,
                modelValue: visible.value,
                'onUpdate:modelValue': (value: boolean) => {
                  visible.value = value;
                },
              })
            : null;
      },
    });
    const wrapper = mount(Host, {
      global: {
        stubs: dialogStubs,
      },
    });
    await nextTick();

    const dialog = wrapper.getComponent(MangoDialog).getComponent(ElDialogStub);
    expect(() => dialogRef.value?.bringToFront()).not.toThrow();
    expect(dialog.props('zIndex')).toBeUndefined();

    visible.value = true;
    await nextTick();
    dialog.vm.$emit('open');
    await nextTick();
    const openZIndex = dialog.props('zIndex') as number;

    dialog.vm.$emit('close');
    expect(() => dialogRef.value?.bringToFront()).not.toThrow();
    await nextTick();
    expect(dialog.props('zIndex')).toBe(openZIndex);

    dialog.vm.$emit('closed');
    const staleExpose = dialogRef.value;
    rendered.value = false;
    await nextTick();
    expect(dialogRef.value).toBeNull();
    expect(() => staleExpose?.bringToFront()).not.toThrow();

    wrapper.unmount();
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
