import { get } from '../utils/request';
import type { AreaNode } from '../components/ChinaArea/types';
import type { ApiId } from '@mango/api-schema';

export interface AreaTreeParams {
  type?: number;
  parentId?: ApiId;
}

export interface AreaTreeResponse {
  code: number;
  msg: string;
  data: AreaNode[];
}

export function getAreaTree(params?: AreaTreeParams): Promise<AreaNode[]> {
  if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true') {
    return Promise.resolve(getMockAreaTree());
  }
  const hasParentId = params?.parentId !== undefined && params.parentId !== '0';
  const url = hasParentId ? '/system/area/children' : '/system/area/tree';
  const requestParams = hasParentId
    ? { parentId: params.parentId }
    : { type: params?.type };
  return get(url, { params: requestParams }).then((res: any) => {
    if (res && Array.isArray(res)) {
      return transformAreaTree(res);
    }
    return res;
  });
}

function transformAreaTree(data: any[]): AreaNode[] {
  return data.map((item) => ({
    id: String(item.id),
    name: item.name,
    parentId: String(item.pid !== undefined ? item.pid : (item.parentId || 0)),
    level: item.level !== undefined ? Number(item.level) : Number(item.areaType),
    hot: item.hot,
    children: item.children?.length ? transformAreaTree(item.children) : undefined,
  }));
}

function getMockAreaTree(): AreaNode[] {
  return [
    {
      id: '110000',
      name: '北京市',
      parentId: '0',
      level: 1,
      hot: '1',
      children: [
        {
          id: '110100',
          name: '市辖区',
          parentId: '110000',
          level: 2,
          children: [
            { id: '110101', name: '东城区', parentId: '110100', level: 3, hot: '1' },
            { id: '110105', name: '朝阳区', parentId: '110100', level: 3, hot: '1' },
          ],
        },
      ],
    },
    {
      id: '310000',
      name: '上海市',
      parentId: '0',
      level: 1,
      hot: '1',
    },
  ];
}
