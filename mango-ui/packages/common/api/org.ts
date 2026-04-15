import { get } from '../utils/request';
import type { OrgNode } from '../components/OrgSelector/types';

export interface OrgTreeParams {
  parentId?: number;
}

export interface OrgTreeResponse {
  code: number;
  msg: string;
  data: OrgNode[];
}

export function getOrgTree(params?: OrgTreeParams): Promise<OrgNode[]> {
  if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true') {
    return Promise.resolve(getMockOrgTree());
  }
  return get('/org/tree', { params }).then((res: any) => {
    if (res && Array.isArray(res)) {
      return transformOrgTree(res);
    }
    return res;
  });
}

function transformOrgTree(data: any[]): OrgNode[] {
  return data.map((item) => ({
    id: item.id,
    name: item.orgName || item.name,
    parentId: item.pid !== undefined ? item.pid : item.parentId,
    sort: item.orgSort !== undefined ? item.orgSort : item.sort,
    children: item.children?.length ? transformOrgTree(item.children) : undefined,
  }));
}

function getMockOrgTree(): OrgNode[] {
  return [
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
            { id: 5, name: '前端组', parentId: 2, sort: 1 },
            { id: 6, name: '后端组', parentId: 2, sort: 2 },
            { id: 7, name: '测试组', parentId: 2, sort: 3 },
          ],
        },
        {
          id: 3,
          name: '产品部',
          parentId: 1,
          sort: 2,
          children: [
            { id: 8, name: '产品设计组', parentId: 3, sort: 1 },
            { id: 9, name: '产品运营组', parentId: 3, sort: 2 },
          ],
        },
      ],
    },
  ];
}
