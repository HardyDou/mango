/**
 * Organization Tree API - 组织架构树
 *
 * Backend API: GET /org/tree?parentId=&type=
 * Response: {code, msg, data: [{id, name, parentId, sort, children[]}]}
 */

import { del, get, post, put } from '@mango/common/utils/request';

export interface SysOrg {
  id: number;
  orgName: string;
  pid: number;
  orgCode?: string;
  orgType?: number;
  orgSort?: number;
  orgStatus?: string;
  tenantId?: number;
  children?: SysOrg[];
}

export type OrgCommand = Omit<SysOrg, 'tenantId' | 'children'>;

export interface OrgTreeParams {
  parentId?: number;
  type?: number;
  includeDisabled?: boolean;
}

export const orgApi = {
  tree: (params?: OrgTreeParams) => get<SysOrg[]>('/org/tree', { params }),
  children: (parentId: number) => get<SysOrg[]>('/org/children', { params: { parentId } }),
  detail: (id: number) => get<SysOrg>('/org/detail', { params: { id } }),
  create: (data: Partial<SysOrg>) => post<number>('/org', toBackendOrg(data)),
  update: (data: Partial<SysOrg>) => put<void>('/org', toBackendOrg(data)),
  delete: (id: number) => del<void>('/org', { params: { id } }),
};

export function getOrgTree(params?: OrgTreeParams): Promise<SysOrg[]> {
  return orgApi.tree(params);
}

function toBackendOrg(data: Partial<SysOrg>): Partial<OrgCommand> {
  return {
    id: data.id,
    pid: data.pid ?? 0,
    orgName: data.orgName,
    orgCode: data.orgCode,
    orgType: data.orgType,
    orgSort: data.orgSort ?? 0,
    orgStatus: data.orgStatus ?? '1',
  };
}
