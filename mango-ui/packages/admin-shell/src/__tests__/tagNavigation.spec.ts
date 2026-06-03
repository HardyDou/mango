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
