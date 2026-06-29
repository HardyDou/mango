import { createRouter, createWebHashHistory } from 'vue-router';
import { Session } from '@mango/common/utils/storage';
import { getShellPinia } from './appBootstrap';
import { useTagsViewRoutes } from './stores/tagsViewRoutes';

export function createMangoAdminRouter() {
  const router = createRouter({
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
        redirect: '/home',
        meta: { isHide: true },
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
      useTagsViewRoutes(getShellPinia()).clearTagsView();
      return true;
    }
    if (!Session.getToken()) {
      return '/login';
    }
    if (to.path === '/') {
      return '/home';
    }
    return true;
  });

  return router;
}

export const router = createMangoAdminRouter();

export default router;
