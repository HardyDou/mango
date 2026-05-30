import { describe, expect, it } from 'vitest';
import { configureMangoAdminShell } from '../config';
import { getLastShellMenuMergeReport, useMenuHost, type ShellMenu } from '../runtime/menuHost';

describe('@mango/admin-shell menu host', () => {
  it('uses capability and business menus when backend menu loading fails', async () => {
    configureMangoAdminShell({
      menu: {
        loader: async () => {
          throw new Error('backend unavailable');
        },
        capabilityMenus: [createMenu('system:user', '/system/user', 'system/user/index')],
        businessMenus: [createMenu('orders:list', '/orders/list', 'orders/list/index')],
        permissions: [],
      },
    });

    const host = useMenuHost();
    await host.loadMenus();

    expect(host.menus.value.some(menu => menu.path === '/home')).toBe(true);
    expect(host.menus.value.some(menu => menu.path === '/system/user')).toBe(true);
    expect(host.menus.value.some(menu => menu.path === '/orders/list')).toBe(true);
    expect(getLastShellMenuMergeReport()?.diagnostics[0]).toContain('Backend menu load failed');
  });

  it('throws backend menu loading failures when no fallback menus exist', async () => {
    configureMangoAdminShell({
      menu: {
        loader: async () => {
          throw new Error('backend unavailable');
        },
        capabilityMenus: [],
        businessMenus: [],
        permissions: [],
      },
    });

    const host = useMenuHost();

    await expect(host.loadMenus()).rejects.toThrow('backend unavailable');
  });
});

function createMenu(menuCode: string, path: string, component: string): ShellMenu {
  return {
    menuId: menuCode,
    appCode: 'internal-admin',
    moduleCode: menuCode.split(':')[0],
    parentId: 0,
    menuType: 2,
    menuName: menuCode,
    menuCode,
    path,
    component,
    sort: 1,
    status: 1,
    visible: 1,
  };
}
