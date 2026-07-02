import { describe, expect, it } from 'vitest';
import {
  createNotFoundRouteMenu,
  filterMenuForRouteByFeatures,
  findUnexpectedTopLevelMenus,
  isRunnableMenu,
  resolveAccessibleMenuPath,
  resolveDirectoryRouteRedirect,
  shouldShowDevCenter,
  MenuTypeEnum,
  toShellRouteMenu,
  type ShellRouteMenu,
  type ShellMenu,
} from '../runtime/menuHost';
import { getRegisteredPageRoutes, registerModulePages } from '@mango/admin-pages/core';

describe('admin-shell menu contract', () => {
  it('shows development center for dev and test deploy environments by default', () => {
    expect(shouldShowDevCenter({ deployEnv: 'dev' })).toBe(true);
    expect(shouldShowDevCenter({ deployEnv: 'test' })).toBe(true);
  });

  it('hides development center for production-like deploy environments by default', () => {
    expect(shouldShowDevCenter({ deployEnv: 'prod' })).toBe(false);
    expect(shouldShowDevCenter({ deployEnv: 'prd' })).toBe(false);
    expect(shouldShowDevCenter({ deployEnv: 'production' })).toBe(false);
  });

  it('lets explicit configuration override deploy environment defaults', () => {
    expect(shouldShowDevCenter({ deployEnv: 'prod', visible: true })).toBe(true);
    expect(shouldShowDevCenter({ deployEnv: 'dev', visible: false })).toBe(false);
  });

  it('marks fallback routes so full-mode verification can reject them', () => {
    const route = createNotFoundRouteMenu('/missing-page');

    expect(route.sourceMenu.meta?.source).toBe('fallback');
    expect(route.path).toBe('/missing-page');
  });

  it('redirects direct directory route visits to the configured runnable child route', () => {
    const menu = createRouteMenu({
      menuType: MenuTypeEnum.DIRECTORY,
      path: '/procurement',
      redirect: '/procurement/orders',
      children: [
        createRouteMenu({
          menuType: MenuTypeEnum.MENU,
          path: '/procurement/orders',
          component: 'procurement/order/index',
        }),
      ],
    });

    expect(resolveDirectoryRouteRedirect(menu, '/procurement')).toBe('/procurement/orders');
  });

  it('falls back to the first accessible child when directory redirect is not visible to current user', () => {
    const menu = createRouteMenu({
      menuType: MenuTypeEnum.DIRECTORY,
      path: '/guarantee',
      redirect: '/guarantee/overview',
      children: [
        createRouteMenu({
          menuType: MenuTypeEnum.MENU,
          path: '/guarantee/risk-review',
          component: 'guarantee/risk-review/index',
        }),
      ],
    });

    expect(resolveAccessibleMenuPath(menu)).toBe('/guarantee/risk-review');
    expect(resolveDirectoryRouteRedirect(menu, '/guarantee')).toBe('/guarantee/risk-review');
  });

  it('falls back to the first accessible child when directory redirect target is not runnable', () => {
    const menu = createRouteMenu({
      menuType: MenuTypeEnum.DIRECTORY,
      path: '/guarantee',
      redirect: '/guarantee/overview',
      children: [
        createRouteMenu({
          menuType: MenuTypeEnum.MENU,
          path: '/guarantee/overview',
        }),
        createRouteMenu({
          menuType: MenuTypeEnum.MENU,
          path: '/guarantee/risk-review',
          component: 'guarantee/risk-review/index',
        }),
      ],
    });

    expect(resolveAccessibleMenuPath(menu)).toBe('/guarantee/risk-review');
    expect(resolveDirectoryRouteRedirect(menu, '/guarantee')).toBe('/guarantee/risk-review');
  });

  it('resolves plain backend routes to their configured redirect child', () => {
    const menu = {
      path: '/link',
      redirect: '/link/favorites',
      children: [
        { path: '/link/favorites' },
        { path: '/link/company' },
        { path: '/link/my-links' },
      ],
    } as ShellRouteMenu;

    expect(resolveAccessibleMenuPath(menu)).toBe('/link/favorites');
  });

  it('falls back from a direct directory route to the first runnable child route', () => {
    const menu = createRouteMenu({
      menuType: MenuTypeEnum.DIRECTORY,
      path: '/procurement',
      children: [
        createRouteMenu({
          menuType: MenuTypeEnum.MENU,
          path: '/procurement/orders',
          component: 'procurement/order/index',
        }),
      ],
    });

    expect(resolveDirectoryRouteRedirect(menu, '/procurement')).toBe('/procurement/orders');
  });

  it('keeps runnable menu clicks on the menu itself', () => {
    const menu = createRouteMenu({
      menuType: MenuTypeEnum.MENU,
      path: '/guarantee/overview',
      component: 'guarantee/overview/index',
      redirect: '/guarantee/risk-review',
    });

    expect(resolveAccessibleMenuPath(menu)).toBe('/guarantee/overview');
    expect(resolveDirectoryRouteRedirect(menu, '/guarantee/overview')).toBe('');
  });

  it('returns empty path when a directory has no accessible page child', () => {
    const menu = createRouteMenu({
      menuType: MenuTypeEnum.DIRECTORY,
      path: '/guarantee',
      redirect: '/guarantee/overview',
      children: [
        createRouteMenu({
          menuType: MenuTypeEnum.DIRECTORY,
          path: '/guarantee/risk',
          children: [],
        }),
      ],
    });

    expect(resolveAccessibleMenuPath(menu)).toBe('');
    expect(resolveDirectoryRouteRedirect(menu, '/guarantee')).toBe('');
  });

  it('does not redirect runnable menu routes so missing component errors remain visible', () => {
    const menu = createRouteMenu({
      menuType: MenuTypeEnum.MENU,
      path: '/procurement/orders',
    });

    expect(resolveDirectoryRouteRedirect(menu, '/procurement/orders')).toBe('');
  });

  it('reports visible page menus missing component instead of treating them as runnable', () => {
    const menu = createShellRouteMenu({
      menuId: 'broken',
      menuName: '缺失页面',
      menuCode: 'broken:page',
      parentId: 0,
      menuType: MenuTypeEnum.MENU,
      path: '/broken',
      component: undefined,
    });

    expect(isRunnableMenu(menu)).toBe(false);
    expect(resolveDirectoryRouteRedirect(menu, '/broken')).toBe('');
    expect(menu.sourceMenu.meta?.menuContractError).toContain('菜单配置缺少 component');
  });

  it('rejects top-level menus that are neither backend menus nor explicit shell menus', () => {
    const backendTopMenus = ['系统管理', '审批中心', '平台能力', '通知中心'];
    const uiTopMenus = ['首页', '系统管理', '审批中心', '平台能力', '通知中心', '开发中心', '自造菜单'];
    const allowedShellMenus = ['首页', '开发中心'];

    expect(findUnexpectedTopLevelMenus(uiTopMenus, backendTopMenus, allowedShellMenus)).toEqual(['自造菜单']);
  });

  it('filters optional feature menus unless the feature is explicitly enabled', () => {
    const menus: ShellMenu[] = [
      {
        menuId: 'system',
        menuName: '系统管理',
        menuCode: 'system',
        parentId: 0,
        menuType: MenuTypeEnum.DIRECTORY,
        path: '/system',
        sort: 1,
        status: 1,
        visible: 1,
        children: [
          {
            menuId: 'tenant',
            menuName: '机构管理',
            menuCode: 'tenant',
            parentId: 'system',
            menuType: MenuTypeEnum.MENU,
            path: '/system/tenant',
            component: 'system/tenant/index',
            sort: 1,
            status: 1,
            visible: 1,
          },
        ],
      },
      {
        menuId: 'notice',
        moduleCode: 'mango-notice',
        menuName: '通知中心',
        menuCode: 'notice',
        parentId: 0,
        menuType: MenuTypeEnum.DIRECTORY,
        path: '/notice',
        sort: 2,
        status: 1,
        visible: 1,
        children: [
          {
            menuId: 'message',
            moduleCode: 'mango-notice',
            menuName: '我的消息',
            menuCode: 'notice:message',
            parentId: 'notice',
            menuType: MenuTypeEnum.MENU,
            path: '/notice/site-message',
            component: 'notice/site-message/index',
            sort: 1,
            status: 1,
            visible: 1,
          },
        ],
      },
    ];

    expect(filterMenuForRouteByFeatures(menus, new Set(['authorization', 'system'])).map(menu => menu.menuName))
      .toEqual(['系统管理']);
    expect(filterMenuForRouteByFeatures(menus, new Set(['authorization', 'system', 'notice'])).map(menu => menu.menuName))
      .toEqual(['系统管理', '通知中心']);
  });

  it('hides deprecated CMS site setting menus from historical backend data', () => {
    const menus: ShellMenu[] = [
      {
        menuId: 'cms',
        moduleCode: 'mango-cms',
        menuName: '站点运营',
        menuCode: 'cms',
        parentId: 0,
        menuType: MenuTypeEnum.DIRECTORY,
        path: '/cms',
        sort: 1,
        status: 1,
        visible: 1,
        children: [
          {
            menuId: 'site',
            moduleCode: 'mango-cms',
            menuName: '站点管理',
            menuCode: 'cms:site',
            parentId: 'cms',
            menuType: MenuTypeEnum.MENU,
            path: '/cms/sites',
            component: 'cms/sites/index',
            sort: 1,
            status: 1,
            visible: 1,
          },
          {
            menuId: 'site-setting',
            moduleCode: 'mango-cms',
            menuName: '站点配置',
            menuCode: 'cms:site-setting',
            parentId: 'cms',
            menuType: MenuTypeEnum.MENU,
            path: '/cms/site-settings',
            component: 'cms/site-settings/index',
            sort: 2,
            status: 1,
            visible: 1,
          },
        ],
      },
    ];

    const filtered = filterMenuForRouteByFeatures(menus, new Set());

    expect(filtered).toHaveLength(1);
    expect(filtered[0].children?.map(menu => menu.menuName)).toEqual(['站点管理']);
  });

  it('keeps package hidden routes registered without exposing them as visible menus', () => {
    registerModulePages({
      moduleCode: 'mango-workflow',
      pages: {
        'workflow/custom-apply/index': async () => ({}),
      },
      routes: [
        {
          path: '/workflow/custom-apply',
          component: 'workflow/custom-apply/index',
          menuName: '自定义申请',
        },
      ],
    });

    const routes = getRegisteredPageRoutes(['mango-workflow']);

    expect(routes).toContainEqual(expect.objectContaining({
      moduleCode: 'mango-workflow',
      path: '/workflow/custom-apply',
      component: 'workflow/custom-apply/index',
      visible: 0,
    }));
    expect(getRegisteredPageRoutes(['mango-notice']).some(route => route.path === '/workflow/custom-apply')).toBe(false);
  });
});

function createRouteMenu(overrides: Partial<ShellRouteMenu>): ShellRouteMenu {
  const path = overrides.path || '/test';
  return {
    path,
    name: overrides.name || path,
    redirect: overrides.redirect,
    meta: {
      title: 'Test',
      ...(overrides.meta || {}),
    },
    sourceMenu: {
      menuId: path,
      menuName: String(overrides.name || path),
      menuCode: String(overrides.name || path),
      parentId: 0,
      menuType: overrides.menuType || MenuTypeEnum.MENU,
      path,
      component: overrides.component,
      sort: 1,
      status: 1,
      visible: 1,
      children: [],
    },
    component: overrides.component,
    children: overrides.children,
  } as ShellRouteMenu;
}

function createShellRouteMenu(menu: Partial<ShellMenu> & Pick<ShellMenu, 'menuId' | 'menuName' | 'menuCode' | 'parentId' | 'menuType' | 'path'>): ShellRouteMenu {
  return toShellRouteMenu({
    appCode: 'internal-admin',
    moduleCode: 'mango-workflow',
    sort: 1,
    status: 1,
    visible: 1,
    keepAlive: 0,
    pageType: 'LOCAL_ROUTE',
    ...menu,
    children: menu.children || [],
  });
}
