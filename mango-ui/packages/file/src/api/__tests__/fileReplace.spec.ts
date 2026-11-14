import { describe, expect, it, vi } from 'vitest';
import type { HttpClient, HttpRequest } from '@mango/api-schema';
import { createFileApi } from '../file';

const MULTIPART_HEADER = { 'Content-Type': 'multipart/form-data' };
const CHUNK_SIZE = 10 * 1024 * 1024;

function createClient(responses: Array<[string, unknown]>) {
  const request = vi.fn(async (config: HttpRequest) => {
    const url = String(config.url ?? '');
    const matched = responses.find(([prefix]) => url === prefix || url.startsWith(prefix));
    return matched?.[1];
  });
  return { request, client: { request } as unknown as HttpClient };
}

function requestAt(request: ReturnType<typeof vi.fn>, index: number): HttpRequest {
  return request.mock.calls[index]?.[0] as HttpRequest;
}

describe('@mango/file 文件信息编辑与内容替换', () => {
  it('updateRecord 以 PUT 提交标准化后的文件信息', async () => {
    const { request, client } = createClient([['/file/files', { id: '55', fileName: 'report-final.docx' }]]);
    const api = createFileApi(client);

    const record = await api.updateRecord({
      id: '55',
      fileName: 'report-final.docx',
      purpose: 'contract',
      bizMeta: { contract: 'A-1' },
      directoryId: '0',
    });

    expect(record.fileName).toBe('report-final.docx');
    const update = requestAt(request, 0);
    expect(update).toMatchObject({ method: 'PUT', url: '/file/files' });
    expect(update.body).toMatchObject({
      id: '55',
      fileName: 'report-final.docx',
      purpose: 'contract',
      bizMeta: '{"contract":"A-1"}',
      directoryId: '0',
    });
  });

  it('小文件替换以 multipart 提交到 replace 入口并携带文件ID', async () => {
    const { request, client } = createClient([['/file/files/replace', { id: '55', fileName: 'report.docx' }]]);
    const api = createFileApi(client);
    const file = new File(['docx'], 'report.docx', { type: 'application/msword' });

    await api.replaceContent(
      file,
      { id: '55', fileName: 'report.docx', purpose: 'contract' },
      { multipartEnabled: false, multipartThreshold: 1 },
    );

    const replace = requestAt(request, 0);
    expect(replace).toMatchObject({
      method: 'POST',
      url: '/file/files/replace',
      headers: MULTIPART_HEADER,
    });
    const body = replace.body as FormData;
    expect(body.get('file')).toBe(file);
    expect(body.get('id')).toBe('55');
    expect(body.get('fileName')).toBe('report.docx');
    expect(body.get('purpose')).toBe('contract');
  });

  it('大文件替换走分片会话并传递 replaceFileId', async () => {
    const { request, client } = createClient([
      ['/file/files/uploads/9/complete', { id: '55', fileName: 'report.docx' }],
      ['/file/files/uploads/9/parts', true],
      ['/file/files/uploads', { sessionId: '9', uploadMode: 'SERVER_CHUNK', chunkSize: CHUNK_SIZE, totalParts: 1 }],
    ]);
    const api = createFileApi(client);
    const content = new Uint8Array(CHUNK_SIZE + 1);
    const file = new File([content], 'report.docx', { type: 'application/msword' });

    await api.replaceContent(
      file,
      { id: '55', fileName: 'report.docx' },
      { multipartEnabled: true, multipartThreshold: 1 },
    );

    const init = requestAt(request, 0);
    expect(init).toMatchObject({ method: 'POST', url: '/file/files/uploads' });
    expect(init.body).toMatchObject({
      replaceFileId: '55',
      fileName: 'report.docx',
      totalParts: 2,
    });
    expect(requestAt(request, 1)).toMatchObject({ method: 'POST', url: '/file/files/uploads/9/parts' });
    expect(requestAt(request, 2)).toMatchObject({ method: 'POST', url: '/file/files/uploads/9/complete' });
  });

  it('无新文件时只更新信息，不触发上传接口', async () => {
    const { request, client } = createClient([['/file/files', { id: '55', fileName: 'report.docx' }]]);
    const api = createFileApi(client);

    await api.updateRecord({ id: '55', fileName: 'report.docx' });

    expect(request).toHaveBeenCalledTimes(1);
    expect(requestAt(request, 0).url).toBe('/file/files');
  });
});
