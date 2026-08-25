import { mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import PendingAttachmentPreview from './PendingAttachmentPreview.vue';

describe('PendingAttachmentPreview', () => {
  beforeEach(() => {
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:pending-attachment'),
      revokeObjectURL: vi.fn(),
    });
  });

  it('为待发送图片创建本地预览并在卸载时释放', () => {
    const file = new File(['image'], 'demo.png', { type: 'image/png' });
    const wrapper = mount(PendingAttachmentPreview, {
      props: { file, type: 'IMAGE' },
      global: { stubs: { ElIcon: { template: '<i><slot /></i>' } } },
    });

    expect(URL.createObjectURL).toHaveBeenCalledWith(file);
    expect(wrapper.get('img').attributes()).toMatchObject({ src: 'blob:pending-attachment', alt: 'demo.png' });
    wrapper.unmount();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:pending-attachment');
  });

  it('音频和普通文件不创建无用途的对象地址', () => {
    const wrapper = mount(PendingAttachmentPreview, {
      props: { file: new File(['audio'], 'demo.mp3', { type: 'audio/mpeg' }), type: 'AUDIO' },
      global: { stubs: { ElIcon: { template: '<i><slot /></i>' } } },
    });

    expect(wrapper.find('img').exists()).toBe(false);
    expect(wrapper.find('video').exists()).toBe(false);
    expect(URL.createObjectURL).not.toHaveBeenCalled();
  });
});
