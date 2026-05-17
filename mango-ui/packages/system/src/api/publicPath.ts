import { request } from '@mango/common/utils/request';

/**
 * Public path types
 */
export interface PublicPath {
  id?: number;
  path: string;
  pathType: number;
  pathTypeName?: string;
  description: string;
  priority: number;
  status: number;
  createTime?: string;
  updateTime?: string;
}

/**
 * Path type options
 */
export const PATH_TYPE_OPTIONS = [
  { label: '匿名访问', value: 1, description: '无需登录即可访问' },
  { label: '登录访问', value: 2, description: '需要登录但不需要特定权限' },
  { label: '权限访问', value: 3, description: '需要登录并拥有特定权限' },
];

/**
 * Get all enabled public paths
 */
export function getPublicPathList() {
  return request.get<PublicPath[]>('/bff/permission/public-path');
}

/**
 * Add a new public path
 */
export function addPublicPath(data: Partial<PublicPath>) {
  return request.post('/bff/permission/public-path', data);
}

/**
 * Update a public path
 */
export function updatePublicPath(id: number, data: Partial<PublicPath>) {
  return request.put('/bff/permission/public-path', data, { params: { id } });
}

/**
 * Delete a public path
 */
export function deletePublicPath(id: number) {
  return request.delete('/bff/permission/public-path', { params: { id } });
}
