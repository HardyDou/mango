import { del, get, post, put } from '@mango/common/utils/request';

export interface MenuPackageVO {
  packageId?: number;
  packageName: string;
  packageCode: string;
  appCode: string;
  status: number;
  sort?: number;
  remark?: string;
  menuIds: number[];
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

  detail: (packageId: number | string) =>
    get<MenuPackageVO>('/authorization/menu-packages/detail', { params: { packageId } }),

  create: (data: MenuPackageVO) =>
    post<number>('/authorization/menu-packages', data),

  update: (data: MenuPackageVO) =>
    put<boolean>('/authorization/menu-packages', data),

  delete: (packageId: number | string) =>
    del<boolean>('/authorization/menu-packages', { params: { packageId } }),
};
