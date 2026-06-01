import { describe, expect, it } from 'vitest';
import {
  createNotFoundRouteMenu,
  filterMenuForRouteByFeatures,
  findUnexpectedTopLevelMenus,
  shouldShowDevCenter,
  MenuTypeEnum,
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
