import type { RouteRecordRaw } from 'vue-router';

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
  component?: () => Promise<any>;
  redirect?: string;
  children?: MenuItem[];
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
    component: () => import('@/views/profile/index.vue'),
  },
  {
    path: '/password',
    name: 'Password',
    meta: { title: '修改密码', icon: 'Lock' },
    component: () => import('@/views/password/index.vue'),
  },
  {
    path: '/demo',
    name: 'Demo',
    meta: { title: '组件库', icon: 'Box' },
    redirect: '/demo/editor',
    children: [
      {
        path: '/demo/editor',
        name: 'DemoEditor',
        meta: { title: '富文本编辑器', icon: 'Document' },
        component: () => import('@/views/demo/components/EditorView.vue'),
      },
      {
        path: '/demo/code-editor',
        name: 'DemoCodeEditor',
        meta: { title: '代码编辑器', icon: 'Edit' },
        component: () => import('@/views/demo/components/CodeEditorView.vue'),
      },
      {
        path: '/demo/upload',
        name: 'DemoUpload',
        meta: { title: '文件上传', icon: 'Upload' },
        component: () => import('@/views/demo/components/UploadView.vue'),
      },
      {
        path: '/demo/charts',
        name: 'DemoCharts',
        meta: { title: '数据图表', icon: 'DataLine' },
        component: () => import('@/views/demo/components/ChartsView.vue'),
      },
      {
        path: '/demo/directive',
        name: 'DemoDirective',
        meta: { title: '功能指令', icon: 'Key' },
        component: () => import('@/views/demo/components/DirectiveView.vue'),
      },
    ],
  },
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
      path: '/',
      name: 'Layout',
      component: () => import('@/layout/index.vue'),
      redirect: '/home',
      children: menuItems.map((item) => menuItemToRoute(item)),
    },
  ];
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
