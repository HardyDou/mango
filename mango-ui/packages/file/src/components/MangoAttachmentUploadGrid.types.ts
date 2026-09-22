export interface MangoAttachmentUploadCategory {
  key: string;
  name: string;
  required?: boolean;
  minFileCount?: number;
  formats?: string[];
  accept?: string;
  maxFileCount?: number;
  maxFileSizeMb?: number;
  allowSingleFileReplacement?: boolean;
  requirementLabel?: string;
  description?: string;
  /** @deprecated 卡片不再渲染该提示，请改用资料分组说明 `description`。 */
  selectionHint?: string;
}

export interface MangoAttachmentUploadFile {
  categoryKey: string;
  fileId: string;
  fileName: string;
  fileExt?: string;
  fileSize?: number;
  previewUrl?: string;
  downloadUrl?: string;
}

export interface MangoAttachmentUploadFileSlotProps {
  file: MangoAttachmentUploadFile;
  category: MangoAttachmentUploadCategory;
}

export interface MangoAttachmentUploadGridExpose {
  validate: () => boolean;
}
