import { describe, it, expect, vi, beforeEach } from 'vitest';

const get = vi.fn();

vi.mock('../../utils/request', () => ({
  get: (...args: unknown[]) => get(...args),
}));

import {
  clearDictOptionCache,
  clearDictTreeCache,
  listDictOptions,
  listDictTree,
} from '../dict';

describe('dict api cache', () => {
  beforeEach(() => {
    get.mockReset();
    clearDictOptionCache();
    clearDictTreeCache();
  });

  it('FLAT 选项默认走缓存，force 与 clearDictOptionCache 可刷新', async () => {
    get.mockResolvedValue([{ label: '启用', value: '1' }]);

    await listDictOptions('sys_normal_disable');
    await listDictOptions('sys_normal_disable');
    expect(get).toHaveBeenCalledTimes(1);

    await listDictOptions('sys_normal_disable', true);
    expect(get).toHaveBeenCalledTimes(2);

    clearDictOptionCache('sys_normal_disable');
    await listDictOptions('sys_normal_disable');
    expect(get).toHaveBeenCalledTimes(3);
  });

  it('TREE 选项默认走缓存，force 与 clearDictTreeCache 可刷新', async () => {
    get.mockResolvedValue([
      {
        label: '办公用品',
        value: 'office_supplies',
        children: [{ label: '纸张', value: 'paper' }],
      },
    ]);

    const first = await listDictTree('system_demo_tree');
    expect(first[0].children?.[0].label).toBe('纸张');
    await listDictTree('system_demo_tree');
    expect(get).toHaveBeenCalledTimes(1);

    await listDictTree('system_demo_tree', true);
    expect(get).toHaveBeenCalledTimes(2);

    clearDictTreeCache('system_demo_tree');
    await listDictTree('system_demo_tree');
    expect(get).toHaveBeenCalledTimes(3);
  });

  it('options 暴露分组并兼容逗号串', async () => {
    get.mockResolvedValue([
      { label: '值1', value: '1', groups: ['ORDER', 'AFTER_SALE'] },
      { label: '值2', value: '2', groups: 'ORDER,AFTER_SALE' },
      { label: '值3', value: '3' },
    ]);

    const options = await listDictOptions('grouped_dict');

    expect(options.map((item) => item.groups)).toEqual([
      ['ORDER', 'AFTER_SALE'],
      ['ORDER', 'AFTER_SALE'],
      [],
    ]);
  });

  it('并发请求同一字典只发起一次', async () => {
    get.mockResolvedValue([{ label: '启用', value: '1' }]);

    await Promise.all([
      listDictOptions('concurrent_dict'),
      listDictOptions('concurrent_dict'),
      listDictOptions('concurrent_dict'),
    ]);

    expect(get).toHaveBeenCalledTimes(1);
  });
});
