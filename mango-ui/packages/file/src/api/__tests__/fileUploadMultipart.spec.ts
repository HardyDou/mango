import { describe, expect, it, vi } from 'vitest';
import type { HttpClient, HttpRequest } from '@mango/api-schema';
import { createFileApi } from '../file';

const MULTIPART_HEADER = { 'Content-Type': 'multipart/form-data' };

function createRecordingClient(response: unknown) {
  const request = vi.fn().mockResolvedValue(response);
  return { request, client: { request } as unknown as HttpClient };
}

function lastRequest(request: ReturnType<typeof vi.fn>): HttpRequest {
  return request.mock.calls[request.mock.calls.length - 1]?.[0] as HttpRequest;
}

describe('@mango/file 上传 multipart Content-Type', () => {
  it('普通上传以 multipart 头提交 FormData，避免被 JSON 默认头改写', async () => {
    const { request, client } = createRecordingClient({ id: '1', fileName: 'small.pdf', fileSize: 8 });
    const api = createFileApi(client);
    const file = new File(['small-pdf'], 'small.pdf', { type: 'application/pdf' });

    await api.upload(file, { purpose: 'attachment' }, { multipartEnabled: false, multipartThreshold: 1 });

    const upload = lastRequest(request);
    expect(upload).toMatchObject({
      method: 'POST',
      url: '/file/files',
      headers: MULTIPART_HEADER,
    });
    expect(upload.body).toBeInstanceOf(FormData);
    expect((upload.body as FormData).get('file')).toBe(file);
  });

  it('批量上传以 multipart 头提交 FormData', async () => {
    const { request, client } = createRecordingClient([]);
    const api = createFileApi(client);
    const files = [new File(['a'], 'a.pdf'), new File(['b'], 'b.pdf')];

    await api.uploadBatch(files, { purpose: 'attachment' });

    const batch = lastRequest(request);
    expect(batch).toMatchObject({
      method: 'POST',
      url: '/file/files/batch',
      headers: MULTIPART_HEADER,
    });
    expect(batch.body).toBeInstanceOf(FormData);
    expect((batch.body as FormData).getAll('files')).toHaveLength(2);
  });

  it('服务端分片接收以 multipart 头提交分片', async () => {
    const { request, client } = createRecordingClient(true);
    const api = createFileApi(client);
    const chunk = new Blob(['chunk'], { type: 'application/octet-stream' });

    await api.uploadServerPart('9', 1, chunk, 'large.bin');

    const part = lastRequest(request);
    expect(part).toMatchObject({
      method: 'POST',
      url: '/file/files/uploads/9/parts',
      headers: MULTIPART_HEADER,
    });
    expect(part.body).toBeInstanceOf(FormData);
    expect((part.body as FormData).get('partNumber')).toBe('1');
  });
});
