import type { RouteRecordRaw } from 'vue-router';

export const staticRoutes: RouteRecordRaw[] = [
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
    name: 'Layout',
    component: () => import('@/layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: '/home',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: {
          title: '首页',
          icon: 'HomeFilled',
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
