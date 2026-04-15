/**
 * Organization Tree API - 组织架构树
 *
 * Backend API: GET /org/tree?parentId=&type=
 * Response: {code, msg, data: [{id, name, parentId, sort, children[]}]}
 */

import { get } from '@mango/common';
import type { OrgNode } from '@mango/common';

export interface OrgTreeParams {
  /** 父节点ID，默认0查根节点 */
  parentId?: number;
}

export interface OrgTreeResponse {
  code: number;
  msg: string;
  data: OrgNode[];
}

/**
 * 获取组织架构树
 * @param params 查询参数
 */
export function getOrgTree(params?: OrgTreeParams): Promise<OrgNode[]> {
  // Mock data for development - backend A2 not ready yet
  if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true') {
    return Promise.resolve(getMockOrgTree(params));
  }
  return get('/org/tree', { params }).then((res: any) => {
    // Transform API response to component format
    if (res && Array.isArray(res)) {
      return transformOrgTree(res);
    }
    return res;
  });
}

/**
 * 转换组织架构数据格式
 * API返回: {id, orgName, pid, orgSort, children}
 * 组件期望: {id, name, parentId, sort, children}
 */
function transformOrgTree(data: any[]): OrgNode[] {
  return data.map((item) => ({
    id: item.id,
    name: item.orgName || item.name,
    parentId: item.pid !== undefined ? item.pid : item.parentId,
    sort: item.orgSort !== undefined ? item.orgSort : item.sort,
    children: item.children && item.children.length > 0 ? transformOrgTree(item.children) : undefined,
  }));
}

/**
 * Mock data for development
 */
function getMockOrgTree(params?: OrgTreeParams): OrgNode[] {
  const mockData: OrgNode[] = [
    {
      id: 1,
      name: '总公司',
      parentId: 0,
      sort: 1,
      children: [
        {
          id: 2,
          name: '技术部',
          parentId: 1,
          sort: 1,
          children: [
            {
              id: 5,
              name: '前端组',
              parentId: 2,
              sort: 1,
            },
            {
              id: 6,
              name: '后端组',
              parentId: 2,
              sort: 2,
            },
            {
              id: 7,
              name: '测试组',
              parentId: 2,
              sort: 3,
            },
          ],
        },
        {
          id: 3,
          name: '产品部',
          parentId: 1,
          sort: 2,
          children: [
            {
              id: 8,
              name: '产品设计组',
              parentId: 3,
              sort: 1,
            },
            {
              id: 9,
              name: '产品运营组',
              parentId: 3,
              sort: 2,
            },
          ],
        },
        {
          id: 4,
          name: '市场部',
          parentId: 1,
          sort: 3,
          children: [
            {
              id: 10,
              name: '市场推广组',
              parentId: 4,
              sort: 1,
            },
            {
              id: 11,
              name: '商务合作组',
              parentId: 4,
              sort: 2,
            },
          ],
        },
      ],
    },
  ];

  return mockData;
}
