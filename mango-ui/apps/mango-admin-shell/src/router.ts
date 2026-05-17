import { createRouter, createWebHashHistory } from 'vue-router';
import { Session } from '@mango/common/utils/storage';

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@mango/auth').then(m => m.LoginView),
    },
    {
      path: '/',
      name: 'Shell',
      component: () => import('./ShellView.vue'),
      meta: { isHide: true },
    },
    {
      path: '/home',
      redirect: '/',
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'ShellMenu',
      component: () => import('./ShellView.vue'),
      meta: { isHide: true },
    },
  ],
});

router.beforeEach((to) => {
  if (to.path === '/login') {
    return true;
  }
  if (!Session.getToken()) {
    return '/login';
  }
  return true;
});

export default router;
