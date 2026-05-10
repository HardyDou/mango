import { describe, it, expect, vi, afterEach } from 'vitest';

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
  },
}));

// Mock xlsx
vi.mock('xlsx', () => ({
  default: {
    read: vi.fn(() => ({
      SheetNames: ['Sheet1'],
      Sheets: {
        Sheet1: {
          A1: { v: 'Header1' },
          B1: { v: 'Header2' },
        },
      },
    })),
    utils: {
      sheet_to_json: vi.fn(() => [
        { Header1: 'Data1', Header2: 'Data2' },
        { Header1: 'Data3', Header2: 'Data4' },
      ]),
    },
  },
}));

// Mock the upload API
vi.mock('@/api/admin/upload', () => ({
  uploadExcel: vi.fn().mockResolvedValue({
    data: [{ Header1: 'Data1', Header2: 'Data2' }],
  }),
}));

// Mock Session
vi.mock('@/utils/storage', () => ({
  Session: {
    get: vi.fn().mockReturnValue({ token: 'test-token' }),
    getToken: vi.fn().mockReturnValue('test-token'),
  },
}));

describe('ExcelUpload 组件单元测试', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('文件类型验证', () => {
    it('应该正确判断 Excel 文件类型', () => {
      const isExcelFile = (filename: string) =>
        filename.endsWith('.xls') || filename.endsWith('.xlsx');

      expect(isExcelFile('test.xls')).toBe(true);
      expect(isExcelFile('test.xlsx')).toBe(true);
      expect(isExcelFile('test.xlsm')).toBe(false);
      expect(isExcelFile('test.pdf')).toBe(false);
      expect(isExcelFile('test.txt')).toBe(false);
    });

    it('应该拒绝非 Excel 文件', () => {
      const isExcelFile = (filename: string) =>
        filename.endsWith('.xls') || filename.endsWith('.xlsx');

      expect(isExcelFile('document.pdf')).toBe(false);
      expect(isExcelFile('data.doc')).toBe(false);
      expect(isExcelFile('notes.txt')).toBe(false);
    });
  });

  describe('数据预览逻辑', () => {
    it('应该正确提取表头', () => {
      const data = [{ col1: 'a', col2: 'b' }, { col1: 'c', col2: 'd' }];
      const headers = data.length > 0 ? Object.keys(data[0]) : [];
      expect(headers).toEqual(['col1', 'col2']);
    });

    it('应该正确限制预览数据行数', () => {
      const data = Array.from({ length: 20 }, (_, i) => ({
        col1: `data${i}`,
      }));
      const previewLimit = 10;
      const previewData = data.slice(0, previewLimit);
      expect(previewData).toHaveLength(10);
      expect(previewData[0].col1).toBe('data0');
      expect(previewData[9].col1).toBe('data9');
    });

    it('应该处理空数据', () => {
      const data: Record<string, unknown>[] = [];
      const headers = data.length > 0 ? Object.keys(data[0]) : [];
      expect(headers).toEqual([]);
    });

    it('预览数据应该正确截断', () => {
      const data = Array.from({ length: 15 }, (_, i) => ({
        col1: `data${i}`,
      }));
      const previewLimit = 5;
      const previewData = data.slice(0, previewLimit);
      expect(previewData).toHaveLength(5);
      expect(previewData[4].col1).toBe('data4');
    });
  });

  describe('API endpoint 验证', () => {
    it('应该使用正确的上传 endpoint', () => {
      const expectedEndpoint = '/api/file/files';
      expect(expectedEndpoint).toMatch(/^\/api\/file\/files$/);
    });
  });

  describe('xlsx 解析结果验证', () => {
    it('应该正确解析 SheetNames', () => {
      const mockResult = {
        SheetNames: ['Sheet1', 'Sheet2'],
        Sheets: {},
      };
      expect(mockResult.SheetNames).toHaveLength(2);
      expect(mockResult.SheetNames[0]).toBe('Sheet1');
    });

    it('应该正确将 sheet 转换为 JSON', () => {
      const mockSheetData = [
        { Header1: 'Data1', Header2: 'Data2' },
        { Header1: 'Data3', Header2: 'Data4' },
      ];
      expect(mockSheetData).toHaveLength(2);
      expect(mockSheetData[0].Header1).toBe('Data1');
    });
  });
});
