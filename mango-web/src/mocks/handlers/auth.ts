/**
 * Auth Mock Handlers - 认证相关
 */

import { http, HttpResponse } from 'msw';
import { mockUser, mockToken, mockMenus } from '../data';
import type { SysMenuVO } from '@/api/admin/menu';

/**
 * 过滤菜单，移除按钮类型（menuType === 2），用于左侧导航
 */
function filterMenuForNav(menus: SysMenuVO[]): SysMenuVO[] {
  return menus
    .filter(menu => menu.menuType !== 2) // 移除按钮类型
    .map(menu => ({
      ...menu,
      children: menu.children ? filterMenuForNav(menu.children) : [],
    }));
}

/**
 * 将嵌套菜单结构扁平化为数组（用于菜单管理页面的 tree table）
 */
function flattenMenus(menus: SysMenuVO[]): SysMenuVO[] {
  const result: SysMenuVO[] = [];

  function traverse(list: SysMenuVO[]) {
    list.forEach((menu) => {
      const { children, ...rest } = menu;
      result.push(rest as SysMenuVO);
      if (children && children.length > 0) {
        traverse(children);
      }
    });
  }

  traverse(menus);
  return result;
}

export const authHandlers = [
  // 登录
  http.post('/api/auth/login', async ({ request }) => {
    const body = await request.json() as { username: string; password: string };
    const { username, password } = body;

    // 模拟登录验证
    if (username === 'admin' && password === 'admin123') {
      return HttpResponse.json({
        code: 200,
        success: true,
        message: '登录成功',
        data: {
          token: mockToken,
          userInfo: mockUser,
        },
      });
    }

    return HttpResponse.json({
      code: 401,
      success: false,
      message: '用户名或密码错误',
      data: null,
    });
  }),

  // 获取用户信息
  http.get('/api/auth/info', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: mockUser,
    });
  }),

  // 登出
  http.post('/api/auth/logout', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: '退出成功',
      data: null,
    });
  }),

  // 获取菜单（支持 /api/auth/menu/list 和 /auth/menu/list 两种路径）
  // 过滤掉按钮类型，只返回目录和菜单类型（用于左侧导航）
  http.get('/api/auth/menu/list', ({ request }) => {
    console.log('[MSW] Intercepted /api/auth/menu/list', request.url);
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: {
        menus: filterMenuForNav(mockMenus),
        currentStorage: 'localStorage',
      },
    });
  }),

  // 获取菜单（兼容不带 /api 前缀的路径）
  // 过滤掉按钮类型，只返回目录和菜单类型（用于左侧导航）
  http.get('/auth/menu/list', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: {
        menus: filterMenuForNav(mockMenus),
        currentStorage: 'localStorage',
      },
    });
  }),

  // 获取菜单树（用于菜单管理页面）- 返回扁平数据
  http.get('/api/auth/menu/tree', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: flattenMenus(mockMenus),
    });
  }),

  // 获取菜单树（兼容不带 /api 前缀的路径）- 返回扁平数据
  http.get('/auth/menu/tree', () => {
    return HttpResponse.json({
      code: 200,
      success: true,
      message: 'success',
      data: flattenMenus(mockMenus),
    });
  }),
];
