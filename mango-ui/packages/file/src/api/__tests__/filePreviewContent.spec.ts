import { afterEach, describe, expect, it, vi } from 'vitest';
import type { HttpClient } from '@mango/api-schema';
import { configureFileApi, createFileApi, downloadFileRecord, fileApi } from '../file';

afterEach(() => {
  vi.restoreAllMocks();
});

describe('file preview content', () => {
  it('does not mutate the legacy singleton when configuring an injected client', async () => {
    const legacyPage = vi.spyOn(fileApi, 'page').mockResolvedValue({ items: [], total: 0, page: 1, pageSize: 10 });
    const request = vi.fn();

    const configured = configureFileApi({ request } as unknown as HttpClient);
    await fileApi.page();

    expect(configured).not.toBe(fileApi);
    expect(legacyPage).toHaveBeenCalledOnce();
    expect(request).not.toHaveBeenCalled();
  });

  it('loads protected preview bytes by file id', async () => {
    const response = new Blob(['pdf'], { type: 'application/pdf' });
    const request = vi.fn().mockResolvedValue(response);
    const api = createFileApi({ request } as unknown as HttpClient);

    await expect(api.previewContent('123')).resolves.toEqual({ data: response });
    expect(request).toHaveBeenCalledWith({
      method: 'GET',
      url: '/file/files/preview-content',
      query: { id: '123' },
      responseType: 'blob',
    });
  });

  it('downloads protected backend content through an authenticated blob request', async () => {
    const response = { data: new Blob(['protected'], { type: 'text/plain' }) };
    const api = {
      ...createFileApi({ request: vi.fn() } as unknown as HttpClient),
      download: vi.fn().mockResolvedValue(response),
      preview: vi.fn(),
    };
    const createObjectUrl = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:protected-download');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    await downloadFileRecord(api, {
      id: '123',
      fileName: 'protected.txt',
      directDownloadUrl: '/api/file/files/download?id=123',
    });

    expect(api.preview).not.toHaveBeenCalled();
    expect(api.download).toHaveBeenCalledWith('123');
    expect(createObjectUrl).toHaveBeenCalledWith(response.data);
    expect(click).toHaveBeenCalledOnce();
  });

  it('keeps external presigned downloads on the direct browser path', async () => {
    const api = {
      ...createFileApi({ request: vi.fn() } as unknown as HttpClient),
      download: vi.fn(),
      preview: vi.fn(),
    };
    const createObjectUrl = vi.spyOn(URL, 'createObjectURL');
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);

    await downloadFileRecord(api, {
      id: '123',
      fileName: 'external.txt',
      directDownloadUrl: 'https://storage.example.com/external.txt?X-Amz-Signature=signed',
    });

    expect(api.preview).not.toHaveBeenCalled();
    expect(api.download).not.toHaveBeenCalled();
    expect(createObjectUrl).not.toHaveBeenCalled();
    expect(click).toHaveBeenCalledOnce();
  });

  it('rejects JSON error responses instead of creating a preview blob', async () => {
    const response = new Blob([JSON.stringify({ message: '没有文件预览权限' })], { type: 'application/json' });
    const api = createFileApi({ request: vi.fn().mockResolvedValue(response) } as unknown as HttpClient);

    await expect(api.previewContent('123')).rejects.toThrow('没有文件预览权限');
  });
});
