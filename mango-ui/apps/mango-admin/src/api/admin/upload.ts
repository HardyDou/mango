/**
 * Upload API
 *
 * File upload related API endpoints.
 */

import { post } from '@mango/common';

export interface UploadResult {
  url: string;
  fileName: string;
  fileSize: number;
}

/**
 * Excel upload result
 */
export interface ExcelUploadResult {
  fileName: string;
  fileSize: number;
  data: Record<string, unknown>[];
}

/**
 * 通用文件上传
 * @param file 文件对象
 * @returns 上传结果
 */
export function uploadFile(file: File): Promise<UploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return post<UploadResult>('/admin/upload/file', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

/**
 * 图片上传
 * @param file 图片文件
 * @returns 上传结果
 */
export function uploadImage(file: File): Promise<UploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return post<UploadResult>('/admin/upload/image', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

/**
 * Excel 上传
 * @param file Excel 文件
 * @returns 上传结果（包含解析后的数据）
 */
export function uploadExcel(file: File): Promise<ExcelUploadResult> {
  const formData = new FormData();
  formData.append('file', file);
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return post<ExcelUploadResult>('/admin/upload/excel', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

/**
 * 多文件上传
 * @param files 文件列表
 * @returns 上传结果列表
 */
export function uploadMultiple(files: File[]): Promise<UploadResult[]> {
  const formData = new FormData();
  files.forEach((file) => {
    formData.append('files', file);
  });
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return post<UploadResult[]>('/admin/upload/multiple', formData as any, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}
