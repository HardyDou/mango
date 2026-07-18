import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '@mango/common';
import { fileApi } from '../file';

afterEach(() => {
  vi.restoreAllMocks();
});

describe('file preview content', () => {
  it('loads protected preview bytes by file id', async () => {
    const response = {
      data: new Blob(['pdf'], { type: 'application/pdf' }),
      headers: { 'content-type': 'application/pdf' },
    };
    const get = vi.spyOn(request, 'get').mockResolvedValue(response);

    await expect(fileApi.previewContent('123')).resolves.toBe(response);
    expect(get).toHaveBeenCalledWith('/file/files/preview-content', {
      params: { id: '123' },
      responseType: 'blob',
      rawResponse: true,
    });
  });

  it('rejects JSON error responses instead of creating a preview blob', async () => {
    vi.spyOn(request, 'get').mockResolvedValue({
      data: new Blob([JSON.stringify({ message: '没有文件预览权限' })], { type: 'application/json' }),
      headers: { 'content-type': 'application/json' },
    });

    await expect(fileApi.previewContent('123')).rejects.toThrow('没有文件预览权限');
  });
});
