import type { FilePreview, FileRecord } from '../api/file';

export interface MangoFilePreviewDialogProps {
  modelValue: boolean;
  title?: string;
  fileId?: string | number;
  file?: FileRecord | null;
  /** 兼容旧调用方的元信息入参，仅用于展示文件名、大小。@deprecated 下一个 major 移除 */
  preview?: FilePreview | null;
  textContent?: string;
  showActions?: boolean;
  loading?: boolean;
  emptyText?: string;
  /**
   * 旧预览地址入参，仅在未传 fileId 时作为兜底展示。
   *
   * @deprecated 下一个 major 移除，改为只传 fileId，渲染类型由后端 renderMode 决定。
   */
  previewUrl?: string;
  /**
   * 旧文档预览地址入参，仅在未传 fileId 时作为兜底展示。
   *
   * @deprecated 下一个 major 移除，改为只传 fileId。
   */
  documentPreviewUrl?: string;
  /**
   * 旧直连预览地址入参，仅在未传 fileId 时作为兜底展示。
   *
   * @deprecated 下一个 major 移除，改为只传 fileId。
   */
  directPreviewUrl?: string;
}

export interface MangoFilePreviewDialogEmits {
  (event: 'update:modelValue', value: boolean): void;
  (event: 'closed'): void;
}
