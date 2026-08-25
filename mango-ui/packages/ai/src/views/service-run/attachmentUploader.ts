import type { AiChatFileRecord } from '@mango/ai-api';
import type { HttpProgress } from '@mango/api-schema';
import type { AttachmentFileType } from './attachmentSupport';

export interface PendingAttachment {
  id: string;
  file: File;
  type: AttachmentFileType;
  status: 'uploading' | 'ready' | 'error';
  progress: number;
  record?: AiChatFileRecord;
  error?: string;
}

type UploadFile = (
  file: File,
  onProgress: (progress: HttpProgress) => void,
  signal: AbortSignal,
) => Promise<AiChatFileRecord>;

export function createAttachmentUploader(uploadFile: UploadFile, errorMessage: (error: unknown) => string) {
  const controllers = new Map<string, AbortController>();

  async function start(attachment: PendingAttachment) {
    cancel(attachment.id);
    const controller = new AbortController();
    controllers.set(attachment.id, controller);
    attachment.status = 'uploading';
    attachment.progress = 0;
    attachment.record = undefined;
    attachment.error = undefined;
    try {
      attachment.record = await uploadFile(
        attachment.file,
        (event) => {
          if (controller.signal.aborted) return;
          attachment.progress = Math.min(
            99,
            Math.round((event.loaded / Math.max(event.total || attachment.file.size, 1)) * 100),
          );
        },
        controller.signal,
      );
      if (controller.signal.aborted) return;
      attachment.progress = 100;
      attachment.status = 'ready';
    } catch (error) {
      if (controller.signal.aborted) return;
      attachment.status = 'error';
      attachment.error = errorMessage(error);
    } finally {
      if (controllers.get(attachment.id) === controller) controllers.delete(attachment.id);
    }
  }

  function cancel(id: string) {
    controllers.get(id)?.abort('附件上传已取消');
    controllers.delete(id);
  }

  function cancelAll() {
    controllers.forEach((controller) => controller.abort('附件上传已取消'));
    controllers.clear();
  }

  return { start, cancel, cancelAll };
}
