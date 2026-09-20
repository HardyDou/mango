import type { FilePreview, FileRecord } from '../api/file';

export interface MangoFilePreviewDialogProps {
  modelValue: boolean;
  title?: string;
  fileId?: string | number;
  file?: FileRecord | null;
  preview?: FilePreview | null;
  textContent?: string;
  showActions?: boolean;
  loading?: boolean;
  emptyText?: string;
}

export interface MangoFilePreviewDialogEmits {
  (event: 'update:modelValue', value: boolean): void;
  (event: 'closed'): void;
}
