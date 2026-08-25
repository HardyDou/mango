import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import ChatFilePart from './ChatFilePart.vue';

const mocks = vi.hoisted(() => ({
  previewChatFile: vi.fn(),
  downloadChatFile: vi.fn(),
}));

vi.mock('../../composables/useAiConfigurationApi', () => ({
  useAiConfigurationApi: () => ({
    previewChatFile: mocks.previewChatFile,
    downloadChatFile: mocks.downloadChatFile,
  }),
}));

describe('ChatFilePart', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.previewChatFile.mockResolvedValue(new Blob(['media']));
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => 'blob:ai-media'),
      revokeObjectURL: vi.fn(),
    });
  });

  it.each([
    ['IMAGE', 'img'],
    ['VIDEO', 'video'],
    ['AUDIO', 'audio'],
  ] as const)('使用文件中心受权内容渲染%s结果', async (type, selector) => {
    const wrapper = mount(ChatFilePart, {
      props: {
        part: { type, fileId: '10', fileName: 'result.bin', contentType: `${type.toLowerCase()}/test`, fileSize: 5 },
      },
      global: {
        stubs: {
          ElButton: { template: '<button><slot /></button>' },
          ElIcon: { template: '<i><slot /></i>' },
        },
      },
    });
    await flushPromises();

    expect(mocks.previewChatFile).toHaveBeenCalledWith('10');
    expect(wrapper.find(selector).attributes('src')).toBe('blob:ai-media');
    wrapper.unmount();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:ai-media');
  });
});
