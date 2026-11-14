import type { FileBizMeta } from '../../api/file';

/** 文件编辑表单值。 */
export interface FileEditForm {
  fileName: string;
  purpose: string;
  accessLevel: string;
  bizType: string;
  bizId: string;
  bizMeta: string;
  directoryId: string;
}

/** 编辑提交的元数据，与后端文字段保持一致：空字符串表示清空可空字段。 */
export interface FileEditMetadata {
  fileName: string;
  purpose: string;
  accessLevel: string;
  bizType: string;
  bizId: string;
  bizMeta: string;
  directoryId: string;
}

export function extensionOf(fileName: string): string {
  const index = fileName.lastIndexOf('.');
  if (index < 0 || index === fileName.length - 1) {
    return '';
  }
  return fileName.slice(index + 1).toLowerCase();
}

export function toBizMetaText(value?: FileBizMeta): string {
  if (!value) {
    return '';
  }
  return typeof value === 'string' ? value : JSON.stringify(value, null, 2);
}

export function buildEditMetadata(form: FileEditForm): FileEditMetadata {
  return {
    fileName: form.fileName.trim(),
    purpose: form.purpose.trim(),
    accessLevel: form.accessLevel,
    bizType: form.bizType.trim(),
    bizId: form.bizId.trim(),
    bizMeta: form.bizMeta.trim(),
    directoryId: form.directoryId,
  };
}

/** 校验编辑输入；返回 null 表示通过。 */
export function validateEditInput(
  metadata: FileEditMetadata,
  recordExt: string,
  options: { newFileSize?: number; maxSize?: number } = {},
): string | null {
  if (!metadata.fileName) {
    return '请输入文件名';
  }
  if (extensionOf(metadata.fileName) !== recordExt) {
    return `文件名扩展名必须与原文件一致（${recordExt || '无扩展名'}）`;
  }
  if (metadata.bizMeta) {
    try {
      JSON.parse(metadata.bizMeta);
    } catch {
      return '业务参数必须是合法 JSON';
    }
  }
  if (options.newFileSize !== undefined && options.maxSize && options.newFileSize > options.maxSize) {
    return '新文件大小超过限制';
  }
  return null;
}
