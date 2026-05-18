import type { UploadUserFile } from 'element-plus';
import type { ApiId } from '@mango/api-schema';

export interface MangoUploadFileMeta {
  id?: ApiId;
  uid?: number;
  name: string;
  url: string;
  fileName: string;
  fileSize: number;
  contentType?: string;
  objectName?: string;
  directPreviewUrl?: string;
  directDownloadUrl?: string;
}

export type MangoUploadModelValue = string | string[] | MangoUploadFileMeta | MangoUploadFileMeta[];

export type MangoUploadUserFile = UploadUserFile & {
  id?: ApiId;
  fileId?: ApiId;
  fileName?: string;
  fileSize?: number;
  contentType?: string;
  objectName?: string;
  directPreviewUrl?: string;
  directDownloadUrl?: string;
};

export function fileToken(id?: ApiId): string {
  return id ? `mango-file:${id}` : '';
}

export function normalizeUploadResult(response: any): MangoUploadFileMeta {
  const data = response?.data && !response?.url ? response.data : response;
  const id = normalizeId(data?.id);
  const url = directUrl(data?.directPreviewUrl)
    || directUrl(data?.url)
    || directUrl(data?.directDownloadUrl)
    || directUrl(data?.downloadUrl)
    || fileToken(id);
  const fileName = data?.fileName || data?.name || url || '';

  return {
    id,
    name: fileName,
    url,
    fileName,
    fileSize: Number(data?.fileSize ?? data?.size ?? 0),
    contentType: data?.contentType,
    objectName: data?.objectName,
    directPreviewUrl: data?.directPreviewUrl,
    directDownloadUrl: data?.directDownloadUrl,
  };
}

function directUrl(value?: string): string {
  if (!value) return '';
  if (/^(https?:)?\/\//i.test(value) || /^(blob|data):/i.test(value)) return value;
  return '';
}

function normalizeId(value: any): ApiId | undefined {
  return value === undefined || value === null || value === '' ? undefined : String(value);
}

export function modelValueToUploadFiles(value?: MangoUploadModelValue): MangoUploadUserFile[] {
  if (!value) return [];
  const values = Array.isArray(value) ? value : [value];

  return values
    .filter(Boolean)
    .map((item, index) => {
      if (typeof item === 'string') {
        return {
          name: fallbackName(item, index),
          url: item,
        };
      }

      return {
        ...item,
        id: item.id,
        fileId: item.id,
        name: item.fileName || item.name || fallbackName(item.url, index),
        url: item.url || fileToken(item.id),
        fileName: item.fileName || item.name,
        fileSize: item.fileSize,
        contentType: item.contentType,
        objectName: item.objectName,
        directPreviewUrl: item.directPreviewUrl,
        directDownloadUrl: item.directDownloadUrl,
      };
    });
}

export function uploadFilesToModelValue(
  files: MangoUploadUserFile[],
  multiple: boolean,
  valueType: 'token' | 'record',
): string | string[] | MangoUploadFileMeta | MangoUploadFileMeta[] | null {
  const values = files
    .map(uploadUserFileToMeta)
    .filter(item => item.url && !item.url.startsWith('blob:'));

  const result = valueType === 'record' ? values : values.map(item => item.url);
  return multiple ? result : result[0] || (valueType === 'record' ? null : '');
}

export function mergeUploadResult(file: MangoUploadUserFile, response: any): MangoUploadUserFile {
  const uploadResult = normalizeUploadResult(response);
  return {
    ...file,
    id: uploadResult.id,
    fileId: uploadResult.id,
    name: uploadResult.fileName || file.name,
    url: uploadResult.url,
    fileName: uploadResult.fileName,
    fileSize: uploadResult.fileSize,
    contentType: uploadResult.contentType,
    objectName: uploadResult.objectName,
    directPreviewUrl: uploadResult.directPreviewUrl,
    directDownloadUrl: uploadResult.directDownloadUrl,
    response,
  };
}

export function normalizeUploadFiles(files: MangoUploadUserFile[]): MangoUploadUserFile[] {
  return files.map((file) => {
    if (!file.response) {
      return file;
    }
    return mergeUploadResult(file, file.response);
  });
}

function uploadUserFileToMeta(file: MangoUploadUserFile): MangoUploadFileMeta {
  const responseMeta = file.response ? normalizeUploadResult(file.response) : null;
  const id = file.id || file.fileId || responseMeta?.id;
  const url = file.url || responseMeta?.url || fileToken(id);
  const fileName = file.fileName || responseMeta?.fileName || file.name || url;

  return {
    id,
    uid: file.uid,
    name: fileName,
    url,
    fileName,
    fileSize: Number(file.fileSize ?? responseMeta?.fileSize ?? file.size ?? 0),
    contentType: file.contentType || responseMeta?.contentType,
    objectName: file.objectName || responseMeta?.objectName,
    directPreviewUrl: file.directPreviewUrl || responseMeta?.directPreviewUrl,
    directDownloadUrl: file.directDownloadUrl || responseMeta?.directDownloadUrl,
  };
}

function fallbackName(url: string, index: number): string {
  if (!url) return `file-${index}`;
  const tokenId = url.startsWith('mango-file:') ? url.replace('mango-file:', '') : '';
  if (tokenId) return `file-${tokenId}`;
  return url.split('/').pop() || `file-${index}`;
}
