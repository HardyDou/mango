import type { RouteRecordRaw } from 'vue-router';
import { MangoAdminLayout } from '@mango/admin-shell';
import { DEV_COMPONENT_DEMO_PAGES, DEV_COMPONENT_DEMO_REDIRECT } from '@mango/admin-pages/dev-component-pages';
import { componentsMap } from './componentsMap';

/**
 * 菜单配置
 * @description 统一管理菜单配置，支持前端路由模式和后端路由模式
 * 如需从接口获取菜单，可在此基础上扩展
 */

export interface MenuMeta {
  title: string;
  icon: string;
  isAffix?: boolean;
  permissions?: string[];
}

export interface MenuItem {
  path: string;
  name: string;
  meta: MenuMeta;
  component?: RouteRecordRaw['component'];
  redirect?: string;
  children?: MenuItem[];
}

function createDevelopMenu(): MenuItem {
  return {
    path: '/develop',
    name: 'DevelopCenter',
    meta: { title: '开发中心', icon: 'Monitor' },
    redirect: DEV_COMPONENT_DEMO_REDIRECT,
    children: [
      {
        path: '/develop/components',
        name: 'DevelopComponents',
        meta: { title: '组件库', icon: 'Box' },
        redirect: DEV_COMPONENT_DEMO_REDIRECT,
        children: DEV_COMPONENT_DEMO_PAGES.map((page) => ({
          path: page.path,
          name: page.menuCode,
          meta: { title: page.menuName, icon: page.icon },
          component: componentsMap[page.component] as NonNullable<RouteRecordRaw['component']>,
        })),
      },
      {
        path: '/develop/capabilities',
        name: 'PlatformCapabilities',
        meta: { title: '平台能力介绍', icon: 'Collection' },
        redirect: '/debug/capabilities/template',
        children: [
          {
            path: '/debug/capabilities/template',
            name: 'TemplateServiceGuide',
            meta: { title: '模板服务', icon: 'Document' },
            component: componentsMap['debug/capabilities/template'] as NonNullable<RouteRecordRaw['component']>,
          },
        ],
      },
    ],
  };
}

/**
 * 菜单配置列表
 */
const menuItems: MenuItem[] = [
  {
    path: '/home',
    name: 'Home',
    meta: { title: '首页', icon: 'HomeFilled', isAffix: true },
    component: () => import('@/views/home/index.vue'),
  },
  {
    path: '/profile',
    name: 'Profile',
    meta: { title: '个人中心', icon: 'User' },
    component: () => import('@mango/auth').then(m => m.ProfileView),
  },
  {
    path: '/password',
    name: 'Password',
    meta: { title: '修改密码', icon: 'Lock' },
    component: () => import('@mango/auth').then(m => m.PasswordView),
  },
  ...(import.meta.env.DEV ? [createDevelopMenu()] : []),
];

/**
 * 生成静态路由配置
 */
export function generateStaticRoutes(): RouteRecordRaw[] {
  return [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@mango/auth').then(m => m.LoginView),
      meta: { title: '登录', isAffix: true },
    },
    {
      path: '/404',
      name: 'NotFound',
      component: () => import('@/views/error/404.vue'),
      meta: { title: '404', isAffix: true },
    },
    {
      path: '/401',
      name: 'NoPermission',
      component: () => import('@/views/error/401.vue'),
      meta: { title: '401', isAffix: true },
    },
    {
      path: '/payment/gateway-result',
      name: 'PaymentGatewayResult',
      component: componentsMap['payment/gateway-result/index'] as NonNullable<RouteRecordRaw['component']>,
      meta: { title: '支付结果确认', isAffix: false },
    },
    {
      path: '/',
      name: 'Layout',
      component: MangoAdminLayout,
      redirect: '/home',
      children: [
        ...menuItems.map((item) => menuItemToRoute(item)),
        paymentCashierRoute(),
      ],
    },
  ];
}

function paymentCashierRoute(): RouteRecordRaw {
  return {
    path: '/payment/cashier-configs/:cashierId/cashier',
    name: 'PaymentCashier',
    component: componentsMap['payment/cashier/index'] as NonNullable<RouteRecordRaw['component']>,
    meta: { title: '收银台', isHide: true },
  };
}

/**
 * 将菜单项转换为路由记录
 */
function menuItemToRoute(item: MenuItem): RouteRecordRaw {
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
    (route as any).children = item.children.map(menuItemToRoute);
  }

  return route;
}

/**
 * 获取菜单配置列表（用于菜单渲染）
 */
export function getMenuConfig(): MenuItem[] {
  return menuItems;
}

/**
 * 生成后端路由模式需要的路由配置
 */
export function generateBackEndRoutes(): RouteRecordRaw[] {
  function menuToRouteWithPermissions(item: MenuItem): RouteRecordRaw {
    const route: RouteRecordRaw = {
      path: item.path,
      name: item.name,
      meta: {
        ...item.meta,
        permissions: ['admin'],
      },
    };

    if (item.component) {
      route.component = item.component;
    }

    if (item.redirect) {
      route.redirect = item.redirect;
    }

    if (item.children && item.children.length > 0) {
      (route as any).children = item.children.map(menuToRouteWithPermissions);
    }

    return route;
  }

  return [
    {
      path: '/',
      name: 'Layout',
      meta: { title: '主布局', permissions: ['admin'] },
      children: menuItems.map(menuToRouteWithPermissions),
    },
  ] as RouteRecordRaw[];
}
