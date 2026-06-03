import { describe, expect, it } from 'vitest';
import { resolveClosedTagFallback } from '../runtime/tagNavigation';

describe('tag navigation', () => {
  it('falls back to the previous tag when closing the active tag', () => {
    const fallback = resolveClosedTagFallback([
      { path: '/home', meta: { isAffix: true } },
      { path: '/system/user' },
      { path: '/procurement/orders' },
    ] as any, { path: '/procurement/orders' } as any, '/procurement/orders');

    expect(fallback).toMatchObject({ path: '/system/user' });
  });

  it('does not carry route record name or meta into fallback navigation', () => {
    const fallback = resolveClosedTagFallback([
      { path: '/home', name: 'home', meta: { isAffix: true } },
      { path: '/system/menu-package', name: 'system:menu-package', meta: { title: '套餐管理' } },
      { path: '/system/tenant', name: 'system:tenant', meta: { title: '机构管理' } },
    ] as any, { path: '/system/tenant' } as any, '/system/tenant');

    expect(fallback).toEqual({ path: '/system/menu-package', query: undefined, hash: undefined, replace: false });
  });

  it('falls back to home when closing the only active non-home tag', () => {
    const fallback = resolveClosedTagFallback([
      { path: '/home', meta: { isAffix: true } },
      { path: '/procurement/orders' },
    ] as any, { path: '/procurement/orders' } as any, '/procurement/orders');

    expect(fallback).toMatchObject({ path: '/home' });
  });

  it('does not navigate when closing an inactive tag', () => {
    const fallback = resolveClosedTagFallback([
      { path: '/home', meta: { isAffix: true } },
      { path: '/system/user' },
      { path: '/procurement/orders' },
    ] as any, { path: '/procurement/orders' } as any, '/system/user');

    expect(fallback).toBeUndefined();
  });
});
