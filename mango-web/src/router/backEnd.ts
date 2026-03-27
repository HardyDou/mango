import type { RouteRecordRaw } from 'vue-router';
import { ElMessage } from 'element-plus';
import { router } from './index';
import { Session } from '@/utils/storage';
import { useUserInfo } from '@/stores/userInfo';
import { useRoutesList } from '@/stores/routesList';
import { useThemeConfig } from '@/stores/themeConfig';
import { getTabBarRoutes } from './frontEnd';

/**
 * 初始化后端控制路由
 * @description 从后端获取路由配置，前端根据权限动态组装
 */
export async function initBackEndControlRoutes(): Promise<void> {
  const storesUserInfo = useUserInfo();
  const storesRoutesList = useRoutesList();
  const storesThemeConfig = useThemeConfig();

  try {
    // 模拟从后端获取路由数据
    // 实际项目中替换为: const res = await getRoutes<UserInfo>({ username: storesUserInfo.userInfos.username });
    const mockRoutes = getMockBackEndRoutes();

    // 过滤权限路由
    const accessRoutes = filterAuthRoutes(mockRoutes);

    // 添加 tabBar 对应的路由（用前端的布局组件包裹）
    const tabBarRoutes = getTabBarRoutes(accessRoutes);

    // 存储路由列表
    storesRoutesList.setRoutesList(accessRoutes);

    // 动态添加路由
    tabBarRoutes.forEach((route) => {
      if (!route.children || route.children.length === 0) {
        // 添加到最顶层
        router.addRoute(route);
      } else {
        // 二级路由需要嵌套在父级路由下
        const parentRoute: RouteRecordRaw = {
          path: route.path,
          name: `${route.name}__parent`,
          component: () => import('@/layout/routerView/parent.vue'),
          children: route.children,
          meta: route.meta,
        };
        router.addRoute(parentRoute);
      }
    });

    // 设置用户信息
    storesUserInfo.setUserInfos({
      ...storesUserInfo.userInfos,
      permissions: mockRoutes.meta?.permissions || [],
    });

    // 更新主题配置中的路由权限模式
    storesThemeConfig.setThemeConfig({
      isRequestRoutes: true,
    });
  } catch (error) {
    console.error('初始化后端路由失败:', error);
    Session.clearSession();
    ElMessage.error('路由加载失败，请重新登录');
    router.push('/login');
  }
}

/**
 * 过滤权限路由（模拟）
 * @description 根据用户权限过滤路由
 */
function filterAuthRoutes(routes: RouteRecordRaw): RouteRecordRaw[] {
  // 实际项目中根据用户权限过滤
  // const { permissions } = useUserInfo().userInfos;
  return routes.children || [];
}

/**
 * 获取模拟后端路由
 * @description 开发阶段使用静态数据模拟后端返回的路由配置
 */
function getMockBackEndRoutes(): RouteRecordRaw {
  return {
    path: '/',
    name: 'Layout',
    meta: {
      title: '主布局',
      permissions: ['admin'],
    },
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
        path: '/system',
        name: 'System',
        meta: {
          title: '系统管理',
          icon: 'Setting',
          permissions: ['admin'],
        },
        children: [
          {
            path: '/system/user',
            name: 'SystemUser',
            component: () => import('@/views/system/user/index.vue'),
            meta: {
              title: '用户管理',
              icon: 'User',
              permissions: ['admin'],
            },
          },
          {
            path: '/system/role',
            name: 'SystemRole',
            component: () => import('@/views/system/role/index.vue'),
            meta: {
              title: '角色管理',
              icon: 'Lock',
              permissions: ['admin'],
            },
          },
          {
            path: '/system/menu',
            name: 'SystemMenu',
            component: () => import('@/views/system/menu/index.vue'),
            meta: {
              title: '菜单管理',
              icon: 'Menu',
              permissions: ['admin'],
            },
          },
        ],
      },
      {
        path: '/components',
        name: 'Components',
        meta: {
          title: '组件示例',
          icon: 'Grid',
          permissions: ['admin'],
        },
        children: [
          {
            path: '/components/icon',
            name: 'ComponentsIcon',
            component: () => import('@/views/components/icon/index.vue'),
            meta: {
              title: '图标',
              icon: 'Coin',
              permissions: ['admin'],
            },
          },
          {
            path: '/components/table',
            name: 'ComponentsTable',
            component: () => import('@/views/components/table/index.vue'),
            meta: {
              title: '表格',
              icon: 'List',
              permissions: ['admin'],
            },
          },
        ],
      },
    ],
  } as RouteRecordRaw;
}
