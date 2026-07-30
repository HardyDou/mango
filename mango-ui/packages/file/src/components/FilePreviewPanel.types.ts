import type { ApiId } from '@mango/api-schema';
import type { FilePreview, FileReference } from '../api/file';

export interface FilePreviewPanelProps {
  fileId?: ApiId | `mango-file:${string}` | null;
  file?: FileReference;
  preview?: FilePreview | null;
  previewProviderUrl?: string;
  previewExternalExtensions?: string[];
  showActions?: boolean;
  fitContainer?: boolean;
}
