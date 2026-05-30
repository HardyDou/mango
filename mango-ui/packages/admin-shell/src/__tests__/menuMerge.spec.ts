import { describe, expect, it } from 'vitest';
import type { ShellMenu } from '../runtime/menuHost';
import { mergeShellMenus } from '../runtime/menuMerge';

const MENU_TYPE_DIRECTORY = 1;
const MENU_TYPE_MENU = 2;

describe('@mango/admin-shell menu merge', () => {
  it('keeps backend menus before capability menu supplements', () => {
    const backend = createMenu('system:user', '/system/user', 'backend-user');
    const capability = createMenu('system:user', '/system/user-capability', 'capability-user');

    const result = mergeShellMenus({
      backendMenus: [backend],
      capabilityMenus: [capability],
    });

    expect(result.menus).toHaveLength(1);
    expect(result.menus[0].path).toBe('/system/user');
    expect(result.menus[0].component).toBe('backend-user');
    expect(result.report.items.some(item => item.action === 'merged' && item.menuCode === 'system:user')).toBe(true);
  });

  it('adds capability menus when backend menus are missing', () => {
    const result = mergeShellMenus({
      backendMenus: [],
      capabilityMenus: [createMenu('file:files', '/file/files', 'file/files/index')],
    });

    expect(result.menus.map(menu => menu.menuCode)).toEqual(['file:files']);
    expect(result.report.items[0]).toMatchObject({
      menuCode: 'file:files',
      source: 'capability',
      action: 'added',
    });
  });

  it('appends business menus without replacing Mango menus', () => {
    const result = mergeShellMenus({
      backendMenus: [createMenu('system:user', '/system/user', 'system/user/index')],
      businessMenus: [createMenu('order:list', '/orders', 'order/list/index')],
    });

    expect(result.menus.map(menu => menu.menuCode)).toEqual(['system:user', 'order:list']);
  });

  it('reports path conflicts and keeps the existing menu', () => {
    const result = mergeShellMenus({
      backendMenus: [createMenu('system:user', '/system/user', 'system/user/index')],
      businessMenus: [createMenu('order:user', '/system/user', 'order/user/index')],
    });

    expect(result.menus.map(menu => menu.menuCode)).toEqual(['system:user']);
    expect(result.report.diagnostics[0]).toMatch(/order:user path \/system\/user conflicts/);
  });

  it('filters menu items by permissions and hides empty directories', () => {
    const result = mergeShellMenus({
      businessMenus: [
        {
          ...createMenu('orders', '/orders', undefined, MENU_TYPE_DIRECTORY),
          children: [
            createMenu('orders:list', '/orders/list', 'orders/index', MENU_TYPE_MENU, ['orders:list']),
            createMenu('orders:audit', '/orders/audit', 'orders/audit', MENU_TYPE_MENU, ['orders:audit']),
          ],
        },
      ],
      permissions: ['orders:list'],
    });

    expect(result.menus).toHaveLength(1);
    expect(result.menus[0].children?.map(menu => menu.menuCode)).toEqual(['orders:list']);
    expect(result.report.items.some(item => item.menuCode === 'orders:audit' && item.action === 'filtered')).toBe(true);
  });
});

function createMenu(
  menuCode: string,
  path: string,
  component?: string,
  menuType = MENU_TYPE_MENU,
  permissions: string[] = [],
): ShellMenu {
  return {
    menuId: menuCode,
    appCode: 'internal-admin',
    moduleCode: menuCode.split(':')[0],
    parentId: 0,
    menuType,
    menuName: menuCode,
    menuCode,
    path,
    component,
    sort: 1,
    status: 1,
    visible: 1,
    permissions,
  };
}
