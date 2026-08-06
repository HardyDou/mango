import { beforeEach, describe, expect, it, vi } from 'vitest';
import { getUploadedFileDetail, normalizeFileAccessUrl, uploadImage } from '../upload';
import { get, post } from '../../utils/request';

vi.mock('../../utils/request', () => ({
  default: { get: vi.fn() },
  get: vi.fn(),
  post: vi.fn(),
}));

describe('upload API contract', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('规范化文件服务返回的绝对地址和 /api 相对地址', () => {
    expect(normalizeFileAccessUrl('https://cdn.example/file.png')).toBe('https://cdn.example/file.png');
    expect(normalizeFileAccessUrl('//cdn.example/file.png')).toBe('//cdn.example/file.png');
    expect(normalizeFileAccessUrl('/api/file/files/preview-content?id=1')).toBe('/api/file/files/preview-content?id=1');
    expect(normalizeFileAccessUrl('/file/files/download?id=1')).toBe('/api/file/files/download?id=1');
  });

  it('上传结果优先提供可预览地址，并保留文件 ID', async () => {
    vi.mocked(post).mockResolvedValue({
      id: '123',
      previewUrl: '/api/file/files/preview-content?id=123',
      downloadUrl: '/file/files/download?id=123',
      fileName: 'image.png',
      fileSize: 1024,
      contentType: 'image/png',
    });

    const result = await uploadImage(new File(['image'], 'image.png', { type: 'image/png' }));

    expect(result.id).toBe('123');
    expect(result.url).toBe('/api/file/files/preview-content?id=123');
    expect(result.previewUrl).toBe('/api/file/files/preview-content?id=123');
    expect(result.downloadUrl).toBe('/api/file/files/download?id=123');
  });

  it('文件详情没有 URL 时回退为持久化 token', async () => {
    vi.mocked(get).mockResolvedValue({ id: '456', fileName: 'report.pdf', fileSize: 2048 });

    const result = await getUploadedFileDetail('456');

    expect(result.url).toBe('mango-file:456');
    expect(result.previewUrl).toBeUndefined();
  });
});
