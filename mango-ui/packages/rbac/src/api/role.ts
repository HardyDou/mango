import { del, get, post, put } from '@mango/common/utils/request';
import type { SysMenuVO } from './menu';

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
  createTime?: string | number[];
  updateTime?: string | number[];
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
  getAssignableMenus: (appCode?: string) => get<SysMenuVO[]>('/authorization/roles/assignable-menus', { params: { appCode } }),
  assignMenus: (roleId: number, menuIds: number[]) => post<boolean>('/authorization/roles/menus', { roleId, menuIds }),
  getSubjectRoles: (subjectId: number) => get<RoleVO[]>('/authorization/roles/subjects', { params: { subjectId } }),
  assignSubjectRoles: (data: {
    subjectId: number;
    appCode?: string;
    realm?: string;
    actorType?: string;
    partyType?: string;
    partyId?: number;
    roleIds: number[];
  }) => post<boolean>('/authorization/roles/subjects', data),
};
