import { describe, expect, it } from 'vitest';
import {
  filterMenusForNavigation,
  resolveActiveMenuPath,
  resolveActiveMenuPaths,
  type ActiveMenuNode,
} from '../layout/navMenu/activeMenuPath';

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

  it('keeps tree ancestors active when menu paths are unrelated', () => {
    const menusWithUnrelatedPaths: ActiveMenuNode[] = [
      {
        path: '/a/b/c/d',
        children: [{ path: '/a/1/2/3' }],
      },
    ];

    expect(resolveActiveMenuPaths(menusWithUnrelatedPaths, '/a/1/2/3')).toEqual(['/a/b/c/d', '/a/1/2/3']);
    expect(resolveActiveMenuPath(menusWithUnrelatedPaths, '/a/1/2/3')).toBe('/a/1/2/3');
  });

  it('highlights the nearest visible ancestor for a hidden route with an unrelated path', () => {
    const menusWithHiddenRoute: ActiveMenuNode[] = [
      {
        path: '/system',
        children: [
          {
            path: '/system/permission',
            children: [{ path: '/system/menu-package', meta: { isHide: true } }],
          },
        ],
      },
    ];

    const visibleMenus = filterMenusForNavigation(menusWithHiddenRoute);

    expect(resolveActiveMenuPath(visibleMenus, '/system/menu-package')).toBe('/system/permission');
    expect(visibleMenus[0].children?.[0].children).toEqual([]);
  });
});
