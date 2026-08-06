import type { FileId } from '../../api/upload';

export type EditorAssetErrorSource = 'upload' | 'paste' | 'drop' | 'preview' | 'serialize';
export type EditorAssetKind = 'image' | 'attachment';

export interface EditorAssetError {
  code: string;
  message: string;
  source: EditorAssetErrorSource;
  fileId?: FileId;
  fileName?: string;
  kind?: EditorAssetKind;
}

export type EditorImageError = EditorAssetError;
