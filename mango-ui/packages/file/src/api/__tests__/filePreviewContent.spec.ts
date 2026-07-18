import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from '@mango/common';
import { downloadFileRecord, fileApi } from '../file';

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

  it('downloads protected backend content through an authenticated blob request', async () => {
    const response = {
      data: new Blob(['protected'], { type: 'text/plain' }),
      headers: { 'content-type': 'text/plain' },
    };
    const download = vi.spyOn(fileApi, 'download').mockResolvedValue(response as any);
    const preview = vi.spyOn(fileApi, 'preview');
    const createObjectUrl = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:protected-download');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    await downloadFileRecord({
      id: '123',
      fileName: 'protected.txt',
      directDownloadUrl: '/api/file/files/download?id=123',
    });

    expect(preview).not.toHaveBeenCalled();
    expect(download).toHaveBeenCalledWith('123');
    expect(createObjectUrl).toHaveBeenCalledWith(response.data);
    expect(click).toHaveBeenCalledOnce();
  });

  it('keeps external presigned downloads on the direct browser path', async () => {
    const download = vi.spyOn(fileApi, 'download');
    const preview = vi.spyOn(fileApi, 'preview');
    const createObjectUrl = vi.spyOn(URL, 'createObjectURL');
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    await downloadFileRecord({
      id: '123',
      fileName: 'external.txt',
      directDownloadUrl: 'https://storage.example.com/external.txt?X-Amz-Signature=signed',
    });

    expect(preview).not.toHaveBeenCalled();
    expect(download).not.toHaveBeenCalled();
    expect(createObjectUrl).not.toHaveBeenCalled();
    expect(click).toHaveBeenCalledOnce();
  });

  it('rejects JSON error responses instead of creating a preview blob', async () => {
    vi.spyOn(request, 'get').mockResolvedValue({
      data: new Blob([JSON.stringify({ message: '没有文件预览权限' })], { type: 'application/json' }),
      headers: { 'content-type': 'application/json' },
    });

    await expect(fileApi.previewContent('123')).rejects.toThrow('没有文件预览权限');
  });
});
