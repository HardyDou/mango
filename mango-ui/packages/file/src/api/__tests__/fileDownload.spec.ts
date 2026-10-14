import { describe, expect, it } from 'vitest';
import { downloadQuery, downloadUrl } from '../file';

describe('文件下载压缩参数', () => {
  it('缺省时只带文件 ID', () => {
    expect(downloadUrl('9')).toBe('/api/file/files/download?id=9');
    expect(downloadQuery('9')).toEqual({ id: '9' });
  });

  it('携带压缩档位', () => {
    expect(downloadUrl('9', { compression: 'high' })).toBe('/api/file/files/download?id=9&compression=high');
    expect(downloadQuery('9', { compression: 'extreme' })).toEqual({ id: '9', compression: 'extreme' });
  });

  it('携带目标体积', () => {
    expect(downloadUrl('9', { compression: 'extreme', perFileTargetSizeBytes: 5242880 })).toBe(
      '/api/file/files/download?id=9&compression=extreme&perFileTargetSizeBytes=5242880',
    );
    expect(downloadQuery('9', { perFileTargetSizeBytes: 1024 })).toEqual({
      id: '9',
      perFileTargetSizeBytes: 1024,
    });
  });
});
