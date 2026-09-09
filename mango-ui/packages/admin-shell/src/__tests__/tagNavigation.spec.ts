import { describe, expect, it } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';
import { normalizeRouteLocation, resolveClosedTagFallback, resolveTagLocation } from '../runtime/tagNavigation';

describe('tag navigation', () => {
  it('falls back to the previous tag when closing the active tag', () => {
    const fallback = resolveClosedTagFallback(
      [{ path: '/home', meta: { isAffix: true } }, { path: '/system/user' }, { path: '/procurement/orders' }] as any,
      { path: '/procurement/orders' } as any,
      '/procurement/orders',
    );

    expect(fallback).toMatchObject({ path: '/system/user' });
  });

  it('does not carry route record name or meta into fallback navigation', () => {
    const fallback = resolveClosedTagFallback(
      [
        { path: '/home', name: 'home', meta: { isAffix: true } },
        { path: '/system/menu-package', name: 'system:menu-package', meta: { title: '套餐管理' } },
        { path: '/system/tenant', name: 'system:tenant', meta: { title: '租户管理' } },
      ] as any,
      { path: '/system/tenant' } as any,
      '/system/tenant',
    );

    expect(fallback).toEqual({
      path: '/system/menu-package',
      params: {},
      query: {},
      hash: undefined,
      replace: false,
    });
  });

  it('falls back to home when closing the only active non-home tag', () => {
    const fallback = resolveClosedTagFallback(
      [{ path: '/home', meta: { isAffix: true } }, { path: '/procurement/orders' }] as any,
      { path: '/procurement/orders' } as any,
      '/procurement/orders',
    );

    expect(fallback).toMatchObject({ path: '/home' });
  });

  it('does not navigate when closing an inactive tag', () => {
    const fallback = resolveClosedTagFallback(
      [{ path: '/home', meta: { isAffix: true } }, { path: '/system/user' }, { path: '/procurement/orders' }] as any,
      { path: '/procurement/orders' } as any,
      '/system/user',
    );

    expect(fallback).toBeUndefined();
  });

  it('keeps route data when resolving a tag location', () => {
    expect(
      resolveTagLocation({
        path: '/workflow/task',
        query: { taskId: 'task-1' },
        params: { instanceId: 'instance-1' },
        hash: '#detail',
      }),
    ).toEqual({
      path: '/workflow/task',
      query: { taskId: 'task-1' },
      params: { instanceId: 'instance-1' },
      hash: '#detail',
      replace: false,
    });
  });

  it('uses the named route when params must be preserved', () => {
    expect(
      resolveTagLocation({
        path: '/workflow/instance/instance-1',
        name: 'workflow-instance',
        params: { instanceId: 'instance-1' },
      }),
    ).toEqual({
      name: 'workflow-instance',
      query: {},
      hash: undefined,
      replace: false,
      params: { instanceId: 'instance-1' },
    });
  });

  it('falls back within the same-path tab set by tab identity', () => {
    const tags = [
      { path: '/home', meta: { isAffix: true } },
      { path: '/workflow/task', query: { taskId: 'task-1' }, meta: { title: '任务 1' } },
      { path: '/workflow/task', query: { taskId: 'task-2' }, meta: { title: '任务 2' } },
    ] as any;

    const fallback = resolveClosedTagFallback(tags, tags[2], tags[2]);

    expect(fallback).toMatchObject({ path: '/workflow/task', query: { taskId: 'task-1' } });
  });

  it('normalizes tags without params or query before router.resolve', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/workflow/task', component: {} }],
    });
    const location = normalizeRouteLocation(
      resolveTagLocation({ path: '/workflow/task', params: undefined, query: undefined }),
    );

    expect(location).toMatchObject({ path: '/workflow/task', params: {}, query: {} });
    expect(() => router.resolve(location)).not.toThrow();
  });

  it('preserves all supported business route identifiers', () => {
    const location = resolveTagLocation({
      path: '/guarantee/inquiry/project-form',
      query: { id: 'inquiry-1', orderId: 'order-1', taskId: 'task-1' },
      params: { instanceId: 'instance-1' },
    });

    expect(location).toMatchObject({
      query: { id: 'inquiry-1', orderId: 'order-1', taskId: 'task-1' },
      params: { instanceId: 'instance-1' },
    });
  });
});
