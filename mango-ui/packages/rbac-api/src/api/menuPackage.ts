import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

export interface MenuPackageVO {
  packageId?: ApiId;
  packageName: string;
  packageCode: string;
  appCode: string;
  status: number;
  sort?: number;
  remark?: string;
  menuIds: ApiId[];
  createTime?: string;
  updateTime?: string;
}

export interface MenuPackageQuery {
  appCode?: string;
  keyword?: string;
  status?: number;
}

export const menuPackageApi = {
  list: (params: MenuPackageQuery = {}) =>
    get<MenuPackageVO[]>('/authorization/menu-packages', { params }),

  detail: (packageId: ApiId) =>
    get<MenuPackageVO>('/authorization/menu-packages/detail', { params: { packageId } }),

  create: (data: MenuPackageVO) =>
    post<ApiId>('/authorization/menu-packages', data),

  update: (data: MenuPackageVO) =>
    put<boolean>('/authorization/menu-packages', data),

  delete: (packageId: ApiId) =>
    del<boolean>('/authorization/menu-packages', { params: { packageId } }),
};
