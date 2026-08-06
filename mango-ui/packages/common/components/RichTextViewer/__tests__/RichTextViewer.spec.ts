import { flushPromises, mount } from '@vue/test-utils';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import RichTextViewer from '../index.vue';
import { getUploadedFileDetail } from '../../../api/upload';

vi.mock('../../../api/upload', () => ({
  getUploadedFileDetail: vi.fn(),
}));

describe('RichTextViewer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getUploadedFileDetail).mockImplementation(async (id) => ({
      id,
      url: `/api/file/files/preview-content?id=${id}`,
      previewUrl: `/api/file/files/preview-content?id=${id}`,
      downloadUrl: `/api/file/files/download?id=${id}`,
      fileName: id === '456' ? '报告.pdf' : 'image.png',
      fileSize: 1024,
    }));
  });

  it('按文件 ID 解析图片和附件的最新地址', async () => {
    const wrapper = mount(RichTextViewer, {
      props: {
        content:
          '<img src="mango-file:123" data-file-id="123" data-file-kind="image">' +
          '<a href="mango-file:456" data-file-id="456" data-file-kind="attachment">报告.pdf</a>',
      },
    });
    await flushPromises();

    expect(getUploadedFileDetail).toHaveBeenCalledWith('123');
    expect(getUploadedFileDetail).toHaveBeenCalledWith('456');
    expect(wrapper.find('img').attributes('src')).toBe('/api/file/files/preview-content?id=123');
    expect(wrapper.find('a').attributes()).toMatchObject({
      href: '/api/file/files/preview-content?id=456',
      target: '_blank',
      rel: 'noopener noreferrer',
    });
  });

  it('保留用户资源地址，同时移除脚本、事件属性和 javascript 链接', async () => {
    const wrapper = mount(RichTextViewer, {
      props: {
        content:
          '<img src="data:image/png;base64,abc" onerror="alert(1)">' +
          '<img src="blob:https://example.com/1"><img src="https://third.example/a.png">' +
          '<a href="javascript:alert(1)" onclick="alert(1)">bad</a><script>alert(1)</script>',
      },
    });
    await flushPromises();

    const html = wrapper.find('.rich-text-viewer__content').html();
    expect(html).toContain('data:image/png;base64,abc');
    expect(html).toContain('blob:https://example.com/1');
    expect(html).toContain('https://third.example/a.png');
    expect(html).not.toContain('onerror');
    expect(html).not.toContain('onclick');
    expect(html).not.toContain('javascript:');
    expect(html).not.toContain('<script');
  });

  it('支持注入公开文件解析器', async () => {
    const resolveFile = vi.fn(async (id: string) => ({
      id,
      url: `https://public.example/${id}`,
      previewUrl: `https://public.example/${id}`,
      fileName: 'public.png',
      fileSize: 1,
    }));
    const wrapper = mount(RichTextViewer, {
      props: {
        content: '<img src="mango-file:789" data-file-id="789">',
        resolveFile,
      },
    });
    await flushPromises();

    expect(resolveFile).toHaveBeenCalledWith('789');
    expect(wrapper.find('img').attributes('src')).toBe('https://public.example/789');
  });
});
