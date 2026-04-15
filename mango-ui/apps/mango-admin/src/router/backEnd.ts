import type { RouteRecordRaw } from 'vue-router';
import { ElMessage } from 'element-plus';
import { router } from './index';
import { Session } from '@mango/common';
import { useUserInfo } from '@/stores/userInfo';
import { useRoutesList } from '@/stores/routesList';
import { usePreferencesStore } from '@/stores/preferences';
import { getTabBarRoutes } from './frontEnd';
import { menuLoader, type MenuItem } from '@/config/menuLoader';

/**
 * 初始化后端控制路由
 * @description 从后端获取路由配置，前端根据权限动态组装
 */
export async function initBackEndControlRoutes(): Promise<void> {
  const storesUserInfo = useUserInfo();
  const storesRoutesList = useRoutesList();
  const preferencesStore = usePreferencesStore();

  try {
    // 从后端加载菜单配置（合并前端配置 + 后端API）
    if (import.meta.env.DEV) console.log('[backEnd] 从后端加载菜单配置...');
    const menuItems = await menuLoader.loadFromBackend();

    if (!menuItems || menuItems.length === 0) {
      throw new Error('菜单数据为空');
    }

    if (import.meta.env.DEV) console.log('[backEnd] 菜单加载成功，共', menuItems.length, '个菜单');

    // 构建路由配置
    const routes = buildRoutes(menuItems);

    // 过滤权限路由（实际项目中根据用户权限过滤）
    const accessRoutes = filterAuthRoutes(routes);

    // 添加 tabBar 对应的路由（用前端的布局组件包裹）
    const tabBarRoutes = getTabBarRoutes(accessRoutes);

    // 存储路由列表
    storesRoutesList.setRoutesList(accessRoutes);

    // 动态添加路由
    // 首先添加布局路由（如果不存在）
    if (!router.hasRoute('Layout')) {
      if (import.meta.env.DEV) console.log('[backEnd] Adding Layout route');
      router.addRoute({
        path: '/',
        name: 'Layout',
        component: () => import('@/layout/index.vue'),
      });
    } else {
      if (import.meta.env.DEV) console.log('[backEnd] Layout route already exists');
    }

    // 清空 Layout 的现有子路由（避免与 staticRoutes 重复）
    const layoutRoute = router.getRoutes().find(r => r.name === 'Layout');
    if (layoutRoute && layoutRoute.children) {
      // 保留布局相关的路由，只清空业务子路由
      layoutRoute.children = [];
      if (import.meta.env.DEV) console.log('[backEnd] Cleared Layout children');
    }

    // 添加通配符路由捕获未匹配的路由，等动态路由加载完成后再处理
    // 这解决刷新页面时动态路由未加载导致的白屏问题
    router.addRoute('Layout', {
      path: ':pathMatch(.*)*',
      redirect: '/',
      meta: { title: 'Redirecting...' }
    });

    if (import.meta.env.DEV) console.log('[backEnd] tabBarRoutes:', tabBarRoutes.map(r => ({ path: r.path, name: r.name })));

    tabBarRoutes.forEach((route) => {
      if (import.meta.env.DEV) console.log('[backEnd] Adding route to Layout:', route.path, route.name);
      if (!route.children || route.children.length === 0) {
        // 添加到布局路由下
        router.addRoute('Layout', route);
      } else {
        // 二级路由需要嵌套在父级路由下
        const parentRoute: RouteRecordRaw = {
          path: route.path,
          name: `${route.name}__parent`,
          component: () => import('@/layout/routerView/parent.vue'),
          children: route.children,
          meta: route.meta,
        };
        router.addRoute('Layout', parentRoute);
      }
    });

    if (import.meta.env.DEV) console.log('[backEnd] Final routes:', router.getRoutes().map(r => ({ path: r.path, name: r.name, children: r.children?.map(c => c.path) })));

    // 设置用户信息
    storesUserInfo.setUserInfos({
      ...storesUserInfo.userInfos,
      permissions: [],
    });

    // 更新主题配置中的路由权限模式
    preferencesStore.isRequestRoutes = true;
  } catch (error) {
    console.error('初始化后端路由失败:', error);
    Session.clearSession();
    ElMessage.error('路由加载失败，请重新登录');
    router.push('/login');
  }
}

/**
 * 构建路由配置
 */
function buildRoutes(menuItems: MenuItem[]): RouteRecordRaw[] {
  return menuItems.map((item) => {
    const route: RouteRecordRaw = {
      path: item.path,
      name: item.name,
      meta: item.meta,
    };

    if (item.component) {
      route.component = item.component;
    }

    if (item.redirect) {
      route.redirect = item.redirect;
    }

    if (item.children && item.children.length > 0) {
      (route as any).children = buildRoutes(item.children);
    }

    return route;
  });
}

/**
 * 过滤权限路由
 * @description 根据用户权限过滤路由
 */
function filterAuthRoutes(routes: RouteRecordRaw[]): RouteRecordRaw[] {
  // 实际项目中根据用户权限过滤
  // const { permissions } = useUserInfo().userInfos;
  return routes;
}
