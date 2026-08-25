import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import AttachmentIntake from './AttachmentIntake.vue';

function dataTransfer(files: File[]) {
  return {
    files,
    types: ['Files'],
    dropEffect: 'none',
  } as unknown as DataTransfer;
}

describe('AttachmentIntake', () => {
  it('拖拽文件时显示接收状态并在松开后一次派发文件', async () => {
    const wrapper = mount(AttachmentIntake, {
      props: { disabled: false, hint: '图片、文件' },
      slots: { default: '<textarea />' },
    });
    const transfer = dataTransfer([new File(['image'], 'demo.png', { type: 'image/png' })]);

    await wrapper.trigger('dragenter', { dataTransfer: transfer });
    expect(wrapper.get('[data-state="ai.service-run.attachment-drop"]').text()).toContain('松开以添加');
    await wrapper.trigger('drop', { dataTransfer: transfer });

    expect(wrapper.find('[data-state="ai.service-run.attachment-drop"]').exists()).toBe(false);
    expect(wrapper.emitted('files')?.[0]?.[0]).toHaveLength(1);
  });

  it('粘贴含文件的剪贴板时接管附件，普通文本粘贴保持默认行为', async () => {
    const wrapper = mount(AttachmentIntake, {
      props: { disabled: false, hint: '图片' },
      slots: { default: '<textarea />' },
    });
    const preventDefault = vi.fn();
    const image = new File(['image'], 'clipboard.png', { type: 'image/png' });

    await wrapper.trigger('paste', { clipboardData: { files: [image] }, preventDefault });
    expect(preventDefault).toHaveBeenCalledOnce();
    expect(wrapper.emitted('files')?.[0]?.[0]).toEqual([image]);

    const textPaste = vi.fn();
    await wrapper.trigger('paste', { clipboardData: { files: [] }, preventDefault: textPaste });
    expect(textPaste).not.toHaveBeenCalled();
  });

  it('禁用时不接收拖拽或粘贴文件', async () => {
    const wrapper = mount(AttachmentIntake, {
      props: { disabled: true, hint: '图片' },
      slots: { default: '<textarea />' },
    });
    const transfer = dataTransfer([new File(['image'], 'demo.png', { type: 'image/png' })]);

    await wrapper.trigger('dragenter', { dataTransfer: transfer });
    await wrapper.trigger('drop', { dataTransfer: transfer });
    await wrapper.trigger('paste', { clipboardData: { files: transfer.files } });

    expect(wrapper.find('[data-state="ai.service-run.attachment-drop"]').exists()).toBe(false);
    expect(wrapper.emitted('files')).toBeUndefined();
  });
});
