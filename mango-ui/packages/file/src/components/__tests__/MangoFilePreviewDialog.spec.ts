import { mount } from '@vue/test-utils';
import { defineComponent, h } from 'vue';
import { describe, expect, it } from 'vitest';
import MangoFilePreviewDialog from '../MangoFilePreviewDialog.vue';

const FilePreviewPanelStub = defineComponent({
  name: 'FilePreviewPanel',
  props: {
    fileId: { type: String, default: '' },
    showActions: Boolean,
    fitContainer: Boolean,
  },
  setup(props) {
    return () => h('div', { 'data-preview-id': props.fileId, 'data-fit': String(props.fitContainer) });
  },
});

describe('MangoFilePreviewDialog', () => {
  it('按文件 ID 渲染可填充弹框的预览面板', () => {
    const wrapper = mount(MangoFilePreviewDialog, {
      props: { modelValue: true, title: '合同预览', fileId: 'file-1' },
      global: {
        stubs: {
          FilePreviewPanel: FilePreviewPanelStub,
          ElEmpty: true,
        },
        directives: { loading: () => undefined },
      },
    });

    const panel = wrapper.findComponent(FilePreviewPanelStub);
    expect(panel.attributes('data-preview-id')).toBe('file-1');
    expect(panel.attributes('data-fit')).toBe('true');
    expect(wrapper.find('[data-title="合同预览"]').exists()).toBe(true);
  });

  it('支持直接展示文本文件内容', () => {
    const wrapper = mount(MangoFilePreviewDialog, {
      props: { modelValue: true, textContent: '第一行\n第二行' },
      global: {
        stubs: { FilePreviewPanel: FilePreviewPanelStub, ElEmpty: true },
        directives: { loading: () => undefined },
      },
    });

    expect(wrapper.find('pre').text()).toContain('第一行');
    expect(wrapper.findComponent(FilePreviewPanelStub).exists()).toBe(false);
  });
});
