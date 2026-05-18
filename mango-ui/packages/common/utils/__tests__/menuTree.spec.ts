import { describe, expect, it } from 'vitest';
import {
  containsMenuPath,
  findTopMenuByPath,
  resolveFirstMenuPath,
  type MangoMenuTreeNode,
} from '../menuTree';

const menus: MangoMenuTreeNode[] = [
  {
    path: '/system',
    redirect: '/system/menu-package',
    children: [
      { path: '/system/menu-package' },
    ],
  },
  {
    path: '/develop',
    redirect: '/components/editor',
    children: [
      {
        path: '/develop/components',
        children: [
          { path: '/components/editor' },
          { path: '/components/upload' },
          { path: '/demo/chat' },
        ],
      },
    ],
  },
];

describe('menuTree', () => {
  it('matches menu ownership by tree relationship instead of top-level path prefix', () => {
    expect(containsMenuPath(menus[1], '/components/upload')).toBe(true);
    expect(containsMenuPath(menus[0], '/components/upload')).toBe(false);
    expect(findTopMenuByPath(menus, '/components/upload')?.path).toBe('/develop');
  });

  it('resolves the first route by redirect before walking children', () => {
    expect(resolveFirstMenuPath(menus[1])).toBe('/components/editor');
  });
});
