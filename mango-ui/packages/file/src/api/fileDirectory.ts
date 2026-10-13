/**
 * File Directory API - 文件逻辑目录。
 */

import type { ApiId, HttpClient } from '@mango/api-schema';
import { getLegacyHttpClient } from './legacyHttpClient';

export type DirectoryId = ApiId;

export interface FileDirectory {
  id: DirectoryId;
  tenantId?: DirectoryId;
  parentId: DirectoryId;
  directoryName: string;
  directoryPath?: string;
  sort?: number;
  status?: number;
  createdTime?: string;
  updatedTime?: string;
  children?: FileDirectory[];
}

export interface SaveFileDirectoryCommand {
  id?: DirectoryId;
  parentId?: DirectoryId;
  directoryName: string;
  sort?: number;
  status?: number;
}

export interface FileDirectoryApi {
  tree(): Promise<FileDirectory[]>;
  create(data: SaveFileDirectoryCommand): Promise<DirectoryId>;
  update(data: SaveFileDirectoryCommand): Promise<boolean>;
  delete(id: DirectoryId): Promise<boolean>;
}

export const rootDirectory: FileDirectory = {
  id: '0',
  parentId: '0',
  directoryName: '全部文件',
  directoryPath: '/',
  sort: 0,
  status: 1,
  children: [],
};

export function createFileDirectoryApi(httpClient?: HttpClient): FileDirectoryApi {
  const transport = httpClient || getLegacyHttpClient();
  return {
    tree: () => transport.request<unknown>({ method: 'GET', url: '/file/directories/tree' }).then(normalizeTree),
    create: (data) =>
      transport.request<DirectoryId, ReturnType<typeof toBackendDirectory>>({
        method: 'POST',
        url: '/file/directories',
        body: toBackendDirectory(data),
      }),
    update: (data) =>
      transport.request<boolean, ReturnType<typeof toBackendDirectory>>({
        method: 'PUT',
        url: '/file/directories',
        body: toBackendDirectory(data),
      }),
    delete: (id) => transport.request<boolean>({ method: 'DELETE', url: '/file/directories', query: { id } }),
  };
}

function toBackendDirectory(data: SaveFileDirectoryCommand) {
  return {
    ...data,
    id: normalizeId(data.id),
    parentId: normalizeId(data.parentId ?? 0),
    status: data.status ?? 1,
    sort: data.sort ?? 0,
  };
}

function normalizeTree(items: unknown): FileDirectory[] {
  return (Array.isArray(items) ? items : []).map(normalizeDirectory);
}

function normalizeDirectory(item: unknown): FileDirectory {
  const record = item && typeof item === 'object' ? (item as Record<string, unknown>) : {};
  return {
    ...record,
    id: normalizeId(record.id),
    parentId: normalizeId(record.parentId ?? 0),
    directoryName: String(record.directoryName || ''),
    children: normalizeTree(record.children),
  } as FileDirectory;
}

function normalizeId(value: unknown): string {
  return value === undefined || value === null ? '' : String(value);
}
