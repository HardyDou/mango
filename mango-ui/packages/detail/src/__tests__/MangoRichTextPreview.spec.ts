/* eslint-disable vue/one-component-per-file */
import { flushPromises, mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import MangoRichTextPreview from '../MangoRichTextPreview.vue';

const { preview, previewContent } = vi.hoisted(() => ({
  preview: vi.fn(),
  previewContent: vi.fn(),
}));

vi.mock('@mango/common', () => ({
  RichTextViewer: defineComponent({
    name: 'RichTextViewer',
    props: { content: { type: String, default: '' } },
    emits: ['preview-request'],
    setup(props) {
      return () => h('div', { 'data-rich-text': '' }, props.content);
    },
  }),
}));

vi.mock('@mango/file', () => ({
  fileApi: { preview, previewContent },
  MangoFilePreviewDialog: defineComponent({
    name: 'MangoFilePreviewDialog',
    props: {
      modelValue: Boolean,
      title: { type: String, default: '' },
      fileId: { type: String, default: '' },
      preview: { type: Object, default: null },
      textContent: { type: String, default: '' },
      loading: Boolean,
      emptyText: { type: String, default: '' },
    },
    emits: ['update:modelValue', 'closed'],
    setup(props) {
      return () => h('div', { 'data-preview-dialog': '', 'data-visible': String(props.modelValue) });
    },
  }),
}));

describe('MangoRichTextPreview', () => {
  beforeEach(() => {
    preview.mockReset();
    previewContent.mockReset();
  });

  it('将非文本文件交给统一文件预览弹框', async () => {
    preview.mockResolvedValue({ fileName: '合同.pdf', fileExt: 'pdf', contentType: 'application/pdf' });
    const wrapper = mount(MangoRichTextPreview, { props: { content: '<p>合同</p>' } });

    wrapper.findComponent({ name: 'RichTextViewer' }).vm.$emit('preview-request', {
      fileId: 'file-1',
      fileName: '合同.pdf',
    });
    await flushPromises();

    expect(preview).toHaveBeenCalledWith('file-1');
    expect(wrapper.findComponent({ name: 'MangoFilePreviewDialog' }).props()).toMatchObject({
      modelValue: true,
      title: '合同.pdf',
      fileId: 'file-1',
      loading: false,
    });
  });

  it('读取文本文件内容，并对失败提供明确空状态', async () => {
    preview.mockResolvedValueOnce({ fileName: '说明.txt', fileExt: 'txt', contentType: 'text/plain' });
    previewContent.mockResolvedValueOnce({ data: new Blob(['预览正文'], { type: 'text/plain' }), headers: {} });
    const wrapper = mount(MangoRichTextPreview);

    wrapper.findComponent({ name: 'RichTextViewer' }).vm.$emit('preview-request', {
      fileId: 'file-text',
      fileName: '说明.txt',
    });
    await flushPromises();
    expect(wrapper.findComponent({ name: 'MangoFilePreviewDialog' }).props('textContent')).toBe('预览正文');

    preview.mockRejectedValueOnce(new Error('network'));
    wrapper.findComponent({ name: 'RichTextViewer' }).vm.$emit('preview-request', {
      fileId: 'file-error',
      fileName: '失败.pdf',
    });
    await flushPromises();
    expect(wrapper.findComponent({ name: 'MangoFilePreviewDialog' }).props()).toMatchObject({
      loading: false,
      emptyText: '文件预览加载失败',
    });
  });
});
