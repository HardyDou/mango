/**
 * Organization Tree API - 组织架构树
 *
 * Backend API: GET /org/tree?parentId=&type=
 * Response: {code, msg, data: [{id, name, parentId, sort, children[]}]}
 */

import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

export interface SysOrg {
  id: ApiId;
  orgName: string;
  pid: ApiId;
  orgCode?: string;
  orgType?: number;
  orgSort?: number;
  orgStatus?: string;
  tenantId?: ApiId;
  children?: SysOrg[];
}

export type OrgCommand = Omit<SysOrg, 'tenantId' | 'children'>;

export interface OrgMemberVO {
  relationId: ApiId;
  memberId: ApiId;
  userId?: ApiId;
  username?: string;
  nickname?: string;
  memberName?: string;
  memberType?: string;
  status?: number;
  orgId: ApiId;
  postId?: ApiId;
  postName?: string;
  postCode?: string;
  primaryFlag?: boolean;
  leaderFlag?: boolean;
}

export interface AddOrgMemberCommand {
  orgId: ApiId;
  memberId: ApiId;
  postId?: ApiId;
  primaryFlag?: boolean;
  leaderFlag?: boolean;
}

export interface UpdateOrgMemberCommand {
  relationId: ApiId;
  postId?: ApiId;
  primaryFlag?: boolean;
  leaderFlag?: boolean;
}

export interface CreateOrgMemberAccountCommand {
  orgId: ApiId;
  postId?: ApiId;
  username: string;
  password?: string;
  nickname?: string;
  email?: string;
  phone?: string;
  status?: number;
  remark?: string;
  primaryFlag?: boolean;
  leaderFlag?: boolean;
}

export interface RestoreOrgMemberAccountCommand {
  orgId: ApiId;
  postId?: ApiId;
  username: string;
  realm?: string;
}

export interface OrgTreeParams {
  parentId?: ApiId;
  type?: number;
  includeDisabled?: boolean;
}

export const orgApi = {
  tree: (params?: OrgTreeParams) => get<SysOrg[]>('/org/tree', { params }),
  children: (parentId: ApiId) => get<SysOrg[]>('/org/children', { params: { parentId } }),
  detail: (id: ApiId) => get<SysOrg>('/org/detail', { params: { id } }),
  create: (data: Partial<SysOrg>) => post<ApiId>('/org', toBackendOrg(data)),
  update: (data: Partial<SysOrg>) => put<boolean>('/org', toBackendOrg(data)),
  delete: (id: ApiId) => del<boolean>('/org', { params: { id } }),
  members: (orgId: ApiId) => get<OrgMemberVO[]>('/org/members', { params: { orgId } }),
  memberScope: (orgId: ApiId) => get<ApiId[]>('/org/member-scope', { params: { orgId } }),
  createMemberAccount: (data: CreateOrgMemberAccountCommand) => post<ApiId>('/org/member-accounts', data),
  restoreMemberAccount: (data: RestoreOrgMemberAccountCommand) => post<ApiId>('/org/member-accounts/restore', data),
  addMember: (orgId: ApiId, data: Omit<AddOrgMemberCommand, 'orgId'>) =>
    post<boolean>('/org/members', { ...data, orgId }),
  updateMember: (data: UpdateOrgMemberCommand) => put<boolean>('/org/members', data),
  removeMember: (relationId: ApiId) => del<boolean>('/org/members', { params: { relationId } }),
  leaders: (orgId: ApiId) => get<ApiId[]>('/org/leader', { params: { orgId } }),
};

export function getOrgTree(params?: OrgTreeParams): Promise<SysOrg[]> {
  return orgApi.tree(params);
}

function toBackendOrg(data: Partial<SysOrg>): Partial<OrgCommand> {
  return {
    id: data.id,
    pid: data.pid ?? '0',
    orgName: data.orgName,
    orgCode: data.orgCode,
    orgType: data.orgType,
    orgSort: data.orgSort ?? 0,
    orgStatus: data.orgStatus ?? '1',
  };
}
