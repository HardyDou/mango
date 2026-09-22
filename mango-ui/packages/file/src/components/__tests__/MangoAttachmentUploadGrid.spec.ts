import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fileApi } from '../../api/file';
import MangoAttachmentUploadGrid from '../MangoAttachmentUploadGrid.vue';

const previewDialogStub = defineComponent({
  name: 'MangoFilePreviewDialog',
  props: { modelValue: Boolean, fileId: { type: String, default: '' } },
  setup(_props, { slots }) {
    return () => h('div', { 'data-preview-dialog': 'true' }, slots.default?.());
  },
});

const categories = [
  {
    key: 'business-license',
    name: '营业执照',
    required: true,
    minFileCount: 1,
    maxFileCount: 1,
    formats: ['pdf'],
  },
  {
    key: 'other',
    name: '其他资料',
    maxFileCount: 2,
    formats: ['pdf'],
  },
];

function mountGrid(props: Record<string, unknown> = {}) {
  return mount(MangoAttachmentUploadGrid, {
    props: { categories, ...props } as never,
    slots: {
      'file-status': ({ file }: { file: { fileName: string } }) =>
        h('span', { 'data-slot-status': file.fileName }, '已校验'),
      'file-note': ({ file }: { file: { fileName: string } }) => h('span', { 'data-slot-note': file.fileName }, '备注'),
    },
    global: {
      stubs: {
        ElIcon: true,
        ElButton: defineComponent({
          inheritAttrs: false,
          setup(_props, { attrs, slots }) {
            return () => h('button', attrs, slots.default?.());
          },
        }),
        ElTag: true,
        MangoFilePreviewDialog: previewDialogStub,
      },
    },
  });
}

describe('MangoAttachmentUploadGrid', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it('按分类渲染空卡片，并保留 grid 布局默认值', () => {
    const wrapper = mountGrid();

    expect(wrapper.findAll('.mango-attachment-upload-card')).toHaveLength(2);
    expect(wrapper.find('.mango-attachment-upload-grid').classes()).toContain('is-grid');
    expect(wrapper.find('[data-record-key="business-license"]').attributes('data-state')).toBe('empty');
  });

  it('校验必填分类缺失时返回 false，完整时返回 true', async () => {
    const wrapper = mountGrid();
    const exposed = wrapper.vm as unknown as { validate: () => boolean };

    expect(exposed.validate()).toBe(false);
    await wrapper.setProps({
      modelValue: [{ categoryKey: 'business-license', fileId: 'file-1', fileName: 'license.pdf' }],
    });
    expect(exposed.validate()).toBe(true);
  });

  it('禁用时不提供上传入口，但仍展示已有文件', () => {
    const wrapper = mountGrid({
      disabled: true,
      modelValue: [{ categoryKey: 'business-license', fileId: 'file-1', fileName: 'license.pdf' }],
    });

    expect(wrapper.find('[data-record-key="business-license"] .mango-attachment-upload-card__empty').exists()).toBe(
      false,
    );
    expect(wrapper.text()).toContain('license.pdf');
    expect(wrapper.find('input[type="file"]').attributes('disabled')).toBeDefined();
  });

  it('上传成功后回写带分类键的文件记录，并触发 success', async () => {
    vi.spyOn(fileApi, 'upload').mockResolvedValue({
      id: 'file-2',
      fileName: 'license.pdf',
      fileExt: 'pdf',
      fileSize: 12,
    });
    const wrapper = mountGrid();
    await wrapper.find('[data-record-key="business-license"] .mango-attachment-upload-card__empty').trigger('click');
    const input = wrapper.find('input[type="file"]');
    const file = new File(['pdf'], 'license.pdf', { type: 'application/pdf' });

    Object.defineProperty(input.element, 'files', { configurable: true, value: [file] });
    await input.trigger('change');
    await vi.waitFor(() => expect(wrapper.emitted('update:modelValue')).toBeTruthy());

    expect(wrapper.emitted('update:modelValue')?.at(-1)?.[0]).toEqual([
      expect.objectContaining({ categoryKey: 'business-license', fileId: 'file-2', fileName: 'license.pdf' }),
    ]);
    expect(wrapper.emitted('success')).toHaveLength(1);
  });

  it('保留 file-status 和 file-note 插槽上下文', () => {
    const wrapper = mountGrid({
      modelValue: [{ categoryKey: 'business-license', fileId: 'file-1', fileName: 'license.pdf' }],
    });

    expect(wrapper.find('[data-slot-status="license.pdf"]').text()).toBe('已校验');
    expect(wrapper.find('[data-slot-note="license.pdf"]').text()).toBe('备注');
  });
});
