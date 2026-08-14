import { describe, expect, it } from 'vitest';
import { resolveActiveMenuPath, type ActiveMenuNode } from '../layout/navMenu/activeMenuPath';

const menus: ActiveMenuNode[] = [
  {
    path: '/develop',
    children: [
      {
        path: '/develop/components',
        children: [{ path: '/components/editor' }, { path: '/components/upload' }, { path: '/components/chat' }],
      },
    ],
  },
];

describe('resolveActiveMenuPath', () => {
  it('keeps the owning menu active on a detail route without its own menu item', () => {
    expect(resolveActiveMenuPath(menus, '/components/upload/detail/123')).toBe('/components/upload');
  });

  it('does not treat a sibling route with the same prefix as a child route', () => {
    expect(resolveActiveMenuPath(menus, '/components/uploader')).toBe('');
  });
});
