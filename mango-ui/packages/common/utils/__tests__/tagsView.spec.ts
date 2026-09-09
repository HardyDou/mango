import { describe, expect, it } from 'vitest';
import { createHomeTag, createTabKey, isHomeTag, normalizeTagsViewRoutes } from '../tagsView';

describe('tagsView', () => {
  it('keeps home tag as the first affix tag', () => {
    const tags = normalizeTagsViewRoutes([
      { path: '/system', name: 'System', meta: { title: '系统管理' } },
      { path: '/home', name: 'CustomHome', meta: { title: '首页' } },
      { path: '/file', name: 'File', meta: { title: '文件中心' } },
    ] as any);

    expect(tags.map((tag) => tag.path)).toEqual(['/home', '/system', '/file']);
    expect(tags[0].meta?.isAffix).toBe(true);
  });

  it('creates home tag when tags do not include it', () => {
    const tags = normalizeTagsViewRoutes([{ path: '/system', name: 'System', meta: { title: '系统管理' } }] as any);

    expect(tags[0]).toMatchObject(createHomeTag());
    expect(isHomeTag(tags[0])).toBe(true);
  });

  it('deduplicates tags by path', () => {
    const tags = normalizeTagsViewRoutes([
      { path: '/file', name: 'FileA', meta: { title: '文件中心' } },
      { path: '/file', name: 'FileB', meta: { title: '文件中心重复' } },
    ] as any);

    expect(tags.map((tag) => tag.path)).toEqual(['/home', '/file']);
    expect(tags[1].name).toBe('FileA');
  });

  it('keeps same-path tags when route data identifies different records', () => {
    const tags = normalizeTagsViewRoutes([
      { path: '/orders/detail', query: { id: 'order-1' }, meta: { title: '订单 1' } },
      { path: '/orders/detail', query: { id: 'order-2' }, meta: { title: '订单 2' } },
    ] as any);

    expect(tags.map((tag) => tag.tabKey)).toEqual([
      createTabKey({ path: '/home' }),
      createTabKey({ path: '/orders/detail', query: { id: 'order-1' } }),
      createTabKey({ path: '/orders/detail', query: { id: 'order-2' } }),
    ]);
  });

  it('creates deterministic keys for reordered query and params objects', () => {
    expect(createTabKey({ path: '/orders/detail', query: { id: '1', mode: 'edit' }, params: { tenant: 't1' } })).toBe(
      createTabKey({ path: '/orders/detail', query: { mode: 'edit', id: '1' }, params: { tenant: 't1' } }),
    );
  });

  it('uses path, query, params and hash as the complete tab identity', () => {
    const base = {
      path: '/guarantee/risk/process',
      query: { taskId: 'task-a', reviewId: 'review-a' },
      params: { processInstanceId: 'process-a' },
      hash: '#form',
    };

    expect(createTabKey({ ...base, name: 'risk-a' })).toBe(createTabKey({ ...base, name: 'risk-renamed' }));
    expect(createTabKey(base)).not.toBe(
      createTabKey({
        ...base,
        query: { taskId: 'task-b', reviewId: 'review-b' },
        params: { processInstanceId: 'process-b' },
      }),
    );
    expect(createTabKey(base)).not.toBe(createTabKey({ ...base, hash: '#approval' }));
  });
});
