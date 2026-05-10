/**
 * Upload API
 *
 * File upload related API endpoints.
 */

import {
  uploadExcel as uploadExcelByFileModule,
  uploadFile as uploadFileByFileModule,
  uploadImage as uploadImageByFileModule,
  uploadMultiple as uploadMultipleByFileModule,
} from '@mango/common';

export interface UploadResult {
  id?: number;
  url: string;
  fileName: string;
  fileSize: number;
  contentType?: string;
  objectName?: string;
}

/**
 * Excel upload result
 */
export interface ExcelUploadResult {
  id?: number;
  url: string;
  fileName: string;
  fileSize: number;
  contentType?: string;
  objectName?: string;
  data: Record<string, unknown>[];
}

/**
 * 通用文件上传
 * @param file 文件对象
 * @returns 上传结果
 */
export function uploadFile(file: File): Promise<UploadResult> {
  return uploadFileByFileModule(file);
}

/**
 * 图片上传
 * @param file 图片文件
 * @returns 上传结果
 */
export function uploadImage(file: File): Promise<UploadResult> {
  return uploadImageByFileModule(file);
}

/**
 * Excel 上传
 * @param file Excel 文件
 * @returns 上传结果（包含解析后的数据）
 */
export function uploadExcel(file: File): Promise<ExcelUploadResult> {
  return uploadExcelByFileModule(file);
}

/**
 * 多文件上传
 * @param files 文件列表
 * @returns 上传结果列表
 */
export function uploadMultiple(files: File[]): Promise<UploadResult[]> {
  return uploadMultipleByFileModule(files);
}
