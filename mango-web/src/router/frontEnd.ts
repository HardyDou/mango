import type { RouteRecordRaw } from 'vue-router';
import { useThemeConfig } from '@/stores/themeConfig';
import { useRoutesList } from '@/stores/routesList';

/**
 * 获取前端路由配置
 * @description 静态路由配置，前端根据路由配置直接渲染
 */
export function getFrontEndRoutes(): RouteRecordRaw[] {
  const storesRoutesList = useRoutesList();

  const routes: RouteRecordRaw[] = [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue'),
      meta: {
        title: '登录',
        isAffix: true,
      },
    },
    {
      path: '/404',
      name: 'NotFound',
      component: () => import('@/views/error/404.vue'),
      meta: {
        title: '404',
        isAffix: true,
      },
    },
    {
      path: '/401',
      name: 'NoPermission',
      component: () => import('@/views/error/401.vue'),
      meta: {
        title: '401',
        isAffix: true,
      },
    },
    {
      path: '/',
      component: () => import('@/layout/index.vue'),
      redirect: '/home',
      children: [
        {
          path: '/home',
          name: 'Home',
          component: () => import('@/views/home/index.vue'),
          meta: {
            title: '首页',
            isAffix: true,
          },
        },
        {
          path: '/profile',
          name: 'Profile',
          component: () => import('@/views/profile/index.vue'),
          meta: {
            title: '个人中心',
            icon: 'User',
          },
        },
        {
          path: '/password',
          name: 'Password',
          component: () => import('@/views/password/index.vue'),
          meta: {
            title: '修改密码',
            icon: 'Lock',
          },
        },
      ],
    },
  ];

  // 添加业务路由（从 routesList store 获取）
  const businessRoutes = storesRoutesList.routesList;
  if (businessRoutes.length > 0) {
    const mainRoute = routes.find((r) => r.path === '/');
    if (mainRoute && mainRoute.children) {
      mainRoute.children = [...mainRoute.children, ...getTabBarRoutes(businessRoutes)];
    }
  }

  return routes;
}

/**
 * 获取带 TabBar 的路由（用于后端路由模式）
 * @description 使用 LayoutParentView 组件包裹路由
 */
export function getTabBarRoutes(routes: RouteRecordRaw[]): RouteRecordRaw[] {
  return routes.map((route) => {
    if (route.children && route.children.length > 0) {
      return {
        ...route,
        component: () => import('@/layout/routerView/parent.vue'),
        children: route.children.map((child) => ({
          ...child,
          component: child.component || (() => import('@/layout/routerView/parent.vue')),
        })),
      };
    }
    return {
      ...route,
      component: () => import('@/layout/routerView/parent.vue'),
    };
  });
}
