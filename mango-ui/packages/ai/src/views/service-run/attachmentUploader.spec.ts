import type { AiChatFileRecord } from '@mango/ai-api';
import { describe, expect, it, vi } from 'vitest';
import { createAttachmentUploader, type PendingAttachment } from './attachmentUploader';

function attachment(): PendingAttachment {
  return {
    id: 'attachment-1',
    file: new File(['note'], 'note.txt', { type: 'text/plain' }),
    type: 'FILE',
    status: 'uploading',
    progress: 0,
  };
}

const record: AiChatFileRecord = {
  id: '10',
  fileName: 'note.txt',
  fileSize: 4,
  contentType: 'text/plain',
};

describe('attachmentUploader', () => {
  it('更新真实上传进度并在完成后保留文件中心记录', async () => {
    const upload = vi.fn(
      async (_file: File, onProgress: (value: { loaded: number; total: number }) => void, _signal: AbortSignal) => {
        onProgress({ loaded: 2, total: 4 });
        return record;
      },
    );
    const item = attachment();

    await createAttachmentUploader(upload, () => '上传失败').start(item);

    expect(item).toMatchObject({ status: 'ready', progress: 100, record });
    expect(upload.mock.calls[0]?.[2]).toBeInstanceOf(AbortSignal);
  });

  it('失败后可使用同一附件原位重试并清理旧错误', async () => {
    const upload = vi.fn().mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce(record);
    const uploader = createAttachmentUploader(upload, () => '网络不可用');
    const item = attachment();

    await uploader.start(item);
    expect(item).toMatchObject({ status: 'error', error: '网络不可用' });
    await uploader.start(item);

    expect(upload).toHaveBeenCalledTimes(2);
    expect(item).toMatchObject({ status: 'ready', progress: 100, record, error: undefined });
  });

  it('移除上传中附件时终止请求且不把取消显示为失败', async () => {
    let rejectUpload: (reason?: unknown) => void = () => undefined;
    const upload = vi.fn(
      (_file: File, _onProgress: (value: { loaded: number; total: number }) => void, signal: AbortSignal) =>
        new Promise<AiChatFileRecord>((_resolve, reject) => {
          rejectUpload = reject;
          signal.addEventListener('abort', () => reject(new DOMException('aborted', 'AbortError')));
        }),
    );
    const uploader = createAttachmentUploader(upload, () => '上传失败');
    const item = attachment();
    const pending = uploader.start(item);

    uploader.cancel(item.id);
    rejectUpload();
    await pending;

    expect(upload.mock.calls[0]?.[2].aborted).toBe(true);
    expect(item.status).toBe('uploading');
    expect(item.error).toBeUndefined();
  });
});
