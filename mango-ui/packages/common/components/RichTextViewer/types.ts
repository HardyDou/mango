import type { FileId, UploadResult } from '../../api/upload';

export type RichTextAssetResolver = (id: FileId) => Promise<UploadResult>;
export type RichTextAssetContentResolver = (id: FileId, asset: UploadResult) => Promise<Blob>;

export interface RichTextViewerPreviewRequest {
  fileId: FileId;
  kind: 'image' | 'attachment';
  fileName: string;
  asset?: UploadResult;
}

export interface RichTextViewerResolveError {
  fileId: FileId;
  error: unknown;
}
