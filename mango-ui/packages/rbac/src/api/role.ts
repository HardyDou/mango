import { del, get, post, put } from '@mango/common';

export interface RoleVO {
  roleId?: number;
  tenantId?: number;
  appCode: string;
  realm: string;
  actorType?: string;
  roleCode: string;
  roleName: string;
  roleType: number;
  status: number;
  sort?: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
  menuIds?: number[];
  permissionIds?: number[];
}

export const roleApi = {
  list: () => get<RoleVO[]>('/authorization/roles'),
  detail: (id: number) => get<RoleVO>('/authorization/roles/detail', { params: { id } }),
  create: (data: RoleVO) => post<number>('/authorization/roles', data),
  update: (data: RoleVO) => put<boolean>('/authorization/roles', data),
  delete: (id: number) => del<boolean>('/authorization/roles', { params: { id } }),
  getMenuIds: (roleId: number) => get<number[]>('/authorization/roles/menus', { params: { roleId } }),
  assignMenus: (roleId: number, menuIds: number[]) => post<boolean>('/authorization/roles/menus', { roleId, menuIds }),
};
