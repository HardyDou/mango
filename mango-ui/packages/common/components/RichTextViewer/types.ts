import type { FileId, UploadResult } from '../../api/upload';

export type RichTextAssetResolver = (id: FileId) => Promise<UploadResult>;

export interface RichTextViewerResolveError {
  fileId: FileId;
  error: unknown;
}
