/**
 * Area Tree API - 行政区划树
 *
 * Backend API: GET /area/tree?type=&parentId=
 * Response: {code, msg, data: [{adcode, name, level, hot, children[]}]}
 */

import { get } from '@mango/common';
import type { AreaNode } from '@mango/common';

export interface AreaTreeParams {
  /** 1-省, 2-市, 3-区/县, 4-街道 */
  type?: number;
  /** 父节点数据库ID，0表示根节点 */
  parentId?: number;
}

export interface AreaTreeResponse {
  code: number;
  msg: string;
  data: AreaNode[];
}

/**
 * 获取行政区划树
 * @param params 查询参数
 */
export function getAreaTree(params?: AreaTreeParams): Promise<AreaNode[]> {
  // Mock data for development - backend A1 not ready yet
  if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true') {
    return Promise.resolve(getMockAreaTree(params));
  }
  return get('/area/tree', { params }).then((res: any) => {
    // 转换API响应格式
    if (res && Array.isArray(res)) {
      return transformAreaTree(res);
    }
    return res;
  });
}

/**
 * 转换区划数据格式
 * API返回: {id, name, pid, level, hot, children}
 * 组件期望: {id, name, parentId, level, hot, children}
 */
function transformAreaTree(data: any[]): AreaNode[] {
  return data.map((item) => ({
    id: item.id,
    name: item.name,
    parentId: item.pid !== undefined ? item.pid : (item.parentId || 0),
    level: item.level,
    hot: item.hot,
    children: item.children && item.children.length > 0 ? transformAreaTree(item.children) : undefined,
  }));
}

/**
 * Mock data for development
 */
function getMockAreaTree(params?: AreaTreeParams): AreaNode[] {
  const mockData: AreaNode[] = [
    {
      adcode: '110000',
      name: '北京市',
      level: 1,
      hot: '1',
      children: [
        {
          adcode: '110100',
          name: '市辖区',
          level: 2,
          children: [
            {
              adcode: '110101',
              name: '东城区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '110102',
              name: '西城区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '110105',
              name: '朝阳区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '110106',
              name: '丰台区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '110107',
              name: '石景山区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '110108',
              name: '海淀区',
              level: 3,
              hot: '1',
            },
          ],
        },
      ],
    },
    {
      adcode: '310000',
      name: '上海市',
      level: 1,
      hot: '1',
      children: [
        {
          adcode: '310100',
          name: '市辖区',
          level: 2,
          children: [
            {
              adcode: '310101',
              name: '黄浦区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '310104',
              name: '徐汇区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '310105',
              name: '长宁区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '310106',
              name: '静安区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '310107',
              name: '普陀区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '310109',
              name: '虹口区',
              level: 3,
              hot: '1',
            },
          ],
        },
      ],
    },
    {
      adcode: '440000',
      name: '广东省',
      level: 1,
      hot: '1',
      children: [
        {
          adcode: '440100',
          name: '广州市',
          level: 2,
          hot: '1',
          children: [
            {
              adcode: '440103',
              name: '荔湾区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '440104',
              name: '越秀区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '440105',
              name: '海珠区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '440106',
              name: '天河区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '440111',
              name: '白云区',
              level: 3,
              hot: '1',
            },
          ],
        },
        {
          adcode: '440300',
          name: '深圳市',
          level: 2,
          hot: '1',
          children: [
            {
              adcode: '440303',
              name: '罗湖区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '440304',
              name: '福田区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '440305',
              name: '南山区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '440306',
              name: '宝安区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '440307',
              name: '龙岗区',
              level: 3,
              hot: '1',
            },
          ],
        },
      ],
    },
    {
      adcode: '330000',
      name: '浙江省',
      level: 1,
      hot: '1',
      children: [
        {
          adcode: '330100',
          name: '杭州市',
          level: 2,
          hot: '1',
          children: [
            {
              adcode: '330102',
              name: '上城区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '330105',
              name: '拱墅区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '330106',
              name: '西湖区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '330108',
              name: '滨江区',
              level: 3,
              hot: '1',
            },
          ],
        },
        {
          adcode: '330200',
          name: '宁波市',
          level: 2,
          hot: '1',
          children: [
            {
              adcode: '330203',
              name: '海曙区',
              level: 3,
              hot: '1',
            },
            {
              adcode: '330205',
              name: '江北区',
              level: 3,
              hot: '1',
            },
          ],
        },
      ],
    },
  ];

  return mockData;
}
