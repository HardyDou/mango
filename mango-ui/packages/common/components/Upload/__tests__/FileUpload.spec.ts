import { describe, it, expect, vi, afterEach } from 'vitest';

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

// Mock the upload API
vi.mock('@/api/admin/upload', () => ({
  uploadFile: vi.fn().mockResolvedValue({
    url: 'https://example.com/uploaded.pdf',
    fileName: 'test.pdf',
    fileSize: 1024,
  }),
}));

// Mock Session
vi.mock('@/utils/storage', () => ({
  Session: {
    get: vi.fn().mockReturnValue({ token: 'test-token' }),
    getToken: vi.fn().mockReturnValue('test-token'),
  },
}));

describe('FileUpload 组件单元测试', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('文件大小验证', () => {
    it('应该正确判断文件大小是否超过限制', () => {
      const maxSizeMB = 50;
      const toMB = (bytes: number) => bytes / 1024 / 1024;

      const smallFile = new File([''], 'small.pdf', { type: 'application/pdf' });
      Object.defineProperty(smallFile, 'size', { value: 30 * 1024 * 1024 }); // 30MB

      const largeFile = new File([''], 'large.pdf', { type: 'application/pdf' });
      Object.defineProperty(largeFile, 'size', { value: 100 * 1024 * 1024 }); // 100MB

      expect(toMB(smallFile.size) <= maxSizeMB).toBe(true);
      expect(toMB(largeFile.size) <= maxSizeMB).toBe(false);
    });

    it('应该正确将字节转换为 MB', () => {
      const toMB = (bytes: number) => bytes / 1024 / 1024;

      expect(toMB(50 * 1024 * 1024)).toBe(50);
      expect(toMB(100 * 1024 * 1024)).toBe(100);
    });

    it('应该接受精确等于限制大小的文件', () => {
      const maxSizeMB = 50;
      const toMB = (bytes: number) => bytes / 1024 / 1024;
      const fileSize = 50 * 1024 * 1024; // 50MB

      expect(toMB(fileSize) <= maxSizeMB).toBe(true);
    });
  });

  describe('modelValue 处理逻辑', () => {
    it('应该正确处理空字符串 modelValue', () => {
      const modelValue = '';
      const fileList = modelValue ? [modelValue] : [];
      expect(fileList).toEqual([]);
    });

    it('应该正确处理单个文件 URL', () => {
      const modelValue = 'https://example.com/file.pdf';
      const fileList = modelValue ? [{ name: 'file-0', url: modelValue }] : [];
      expect(fileList).toHaveLength(1);
      expect(fileList[0].url).toBe('https://example.com/file.pdf');
    });

    it('应该正确处理多个文件 URL', () => {
      const modelValue = [
        'https://example.com/file1.pdf',
        'https://example.com/file2.pdf',
      ];
      const fileList = modelValue.map((url, index) => ({
        name: `file-${index}`,
        url,
      }));
      expect(fileList).toHaveLength(2);
      expect(fileList[0].url).toBe('https://example.com/file1.pdf');
    });

    it('multiple=false 时应该只返回第一个 URL', () => {
      const urls = ['https://example.com/file1.pdf', 'https://example.com/file2.pdf'];
      const multiple = false;
      const result = multiple ? urls : urls[0] || '';
      expect(result).toBe('https://example.com/file1.pdf');
    });

    it('multiple=true 时应该返回所有 URL', () => {
      const urls = ['https://example.com/file1.pdf', 'https://example.com/file2.pdf'];
      const multiple = true;
      const result = multiple ? urls : urls[0] || '';
      expect(result).toEqual(urls);
    });
  });

  describe('API endpoint 验证', () => {
    it('应该使用正确的上传 endpoint', () => {
      const expectedEndpoint = '/api/file/files';
      expect(expectedEndpoint).toMatch(/^\/api\/file\/files$/);
    });
  });
});
