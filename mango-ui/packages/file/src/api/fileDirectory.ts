/**
 * File Directory API - 文件逻辑目录。
 */

import { del, get, post, put } from '@mango/common';
import type { ApiId } from '@mango/api-schema';

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

export const rootDirectory: FileDirectory = {
  id: '0',
  parentId: '0',
  directoryName: '全部文件',
  directoryPath: '/',
  sort: 0,
  status: 1,
  children: [],
};

export const fileDirectoryApi = {
  tree: () => get<FileDirectory[]>('/file/directories/tree').then((items: any) => normalizeTree(items)),
  create: (data: SaveFileDirectoryCommand) => post<DirectoryId>('/file/directories', toBackendDirectory(data)),
  update: (data: SaveFileDirectoryCommand) => put<boolean>('/file/directories', toBackendDirectory(data)),
  delete: (id: DirectoryId) => del<boolean>('/file/directories', { params: { id } }),
};

function toBackendDirectory(data: SaveFileDirectoryCommand) {
  return {
    ...data,
    id: normalizeId(data.id),
    parentId: normalizeId(data.parentId ?? 0),
    status: data.status ?? 1,
    sort: data.sort ?? 0,
  };
}

function normalizeTree(items: any[]): FileDirectory[] {
  return (Array.isArray(items) ? items : []).map(normalizeDirectory);
}

function normalizeDirectory(item: any): FileDirectory {
  return {
    ...item,
    id: normalizeId(item.id),
    parentId: normalizeId(item.parentId ?? 0),
    directoryName: item.directoryName || '',
    children: normalizeTree(item.children),
  };
}

function normalizeId(value: any): string {
  return value === undefined || value === null ? '' : String(value);
}
