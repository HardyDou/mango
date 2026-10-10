import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';
import type { SysMenuVO } from './menu';

export interface RoleVO {
  roleId?: ApiId;
  tenantId?: ApiId;
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
  menuIds?: ApiId[];
  permissionIds?: ApiId[];
}

export type DataScopeMode = 'ALL' | 'SELF' | 'SELF_ORG' | 'SELF_ORG_AND_CHILDREN' | 'ORG';

export interface RoleDataScopeVO {
  id?: ApiId;
  tenantId?: ApiId;
  appCode?: string;
  roleId: ApiId;
  resourceCode: string;
  scopeMode: DataScopeMode;
  scopeValues?: string[];
  includeChildren?: boolean;
  status?: number;
  createTime?: string;
  updateTime?: string;
}

export interface SaveRoleDataScopeCommand {
  roleId: ApiId;
  resourceCode: string;
  scopeMode: DataScopeMode;
  scopeValues?: string[];
  includeChildren?: boolean;
  status?: number;
}

export interface SubjectRoleSummaryVO {
  subjectId: ApiId;
  roles: RoleVO[];
}

export type BatchRoleTargetScope =
  'ALL_ENABLED_MEMBERS' | 'SUBJECT_IDS' | 'ORGANIZATION' | 'ORGANIZATION_AND_DESCENDANTS';

export interface BatchRoleAssignmentCommand {
  roleCode: string;
  targetScope: BatchRoleTargetScope;
  subjectIds?: ApiId[];
  orgId?: ApiId;
}

export interface BatchRoleAssignmentPreviewVO {
  roleId: ApiId;
  roleCode: string;
  roleName: string;
  targetScope: BatchRoleTargetScope;
  targetCount: number;
  members: Array<{
    subjectId: ApiId;
    memberNo?: string;
    displayName?: string;
    primaryOrgId?: ApiId;
  }>;
}

export interface BatchRoleAssignmentResultVO {
  roleId: ApiId;
  roleCode: string;
  targetCount: number;
  createdCount: number;
  existingCount: number;
  removedCount: number;
  skippedCount: number;
  skippedSubjectIds?: ApiId[];
}

export const roleApi = {
  list: () => get<RoleVO[]>('/authorization/roles'),
  detail: (id: ApiId) => get<RoleVO>('/authorization/roles/detail', { params: { id } }),
  create: (data: RoleVO) => post<ApiId>('/authorization/roles', data),
  update: (data: RoleVO) => put<boolean>('/authorization/roles', data),
  delete: (id: ApiId) => del<boolean>('/authorization/roles', { params: { id } }),
  getMenuIds: (roleId: ApiId) => get<ApiId[]>('/authorization/roles/menus', { params: { roleId } }),
  getAssignableMenus: (appCode?: string) =>
    get<SysMenuVO[]>('/authorization/roles/assignable-menus', { params: { appCode } }),
  assignMenus: (roleId: ApiId, menuIds: ApiId[]) => post<boolean>('/authorization/roles/menus', { roleId, menuIds }),
  getSubjectRoles: (subjectId: ApiId) => get<RoleVO[]>('/authorization/roles/subjects', { params: { subjectId } }),
  getSubjectRolesBatch: (subjectIds: ApiId[]) =>
    post<SubjectRoleSummaryVO[]>('/authorization/roles/subjects/batch', { subjectIds }),
  assignSubjectRoles: (data: {
    subjectId: ApiId;
    appCode?: string;
    realm?: string;
    actorType?: string;
    partyType?: string;
    partyId?: ApiId;
    roleIds: ApiId[];
  }) => post<boolean>('/authorization/roles/subjects', data),
  previewBatchRoleAssignment: (data: BatchRoleAssignmentCommand) =>
    post<BatchRoleAssignmentPreviewVO>('/authorization/roles/batch/preview', data),
  assignBatchRole: (data: BatchRoleAssignmentCommand) =>
    post<BatchRoleAssignmentResultVO>('/authorization/roles/batch/assign', data),
  unassignBatchRole: (data: BatchRoleAssignmentCommand) =>
    post<BatchRoleAssignmentResultVO>('/authorization/roles/batch/unassign', data),
  getDataScopes: (roleId: ApiId) => get<RoleDataScopeVO[]>('/authorization/data-scopes/roles', { params: { roleId } }),
  saveDataScope: (data: SaveRoleDataScopeCommand) => post<boolean>('/authorization/data-scopes/roles', data),
  deleteDataScope: (roleId: ApiId, resourceCode: string) =>
    del<boolean>('/authorization/data-scopes/roles', {
      params: { roleId, resourceCode },
    }),
};
