import { createRouter, createWebHashHistory } from 'vue-router';
import { Session } from '@mango/common';

export const LOGIN_REDIRECT_KEY = 'MANGO_WORKFLOW_LOGIN_REDIRECT';

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'WorkflowStandalone',
      component: () => import('./StandaloneRoot.vue'),
    },
    {
      path: '/home',
      name: 'WorkflowLoginLanding',
      component: () => import('./StandaloneRoot.vue'),
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@mango/auth').then((m) => m.LoginView),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'WorkflowStandaloneMenu',
      component: () => import('./StandaloneRoot.vue'),
    },
  ],
});

router.beforeEach((to) => {
  if (to.path === '/login') {
    if (!Session.getToken()) {
      return true;
    }
    const redirect = sessionStorage.getItem(LOGIN_REDIRECT_KEY);
    sessionStorage.removeItem(LOGIN_REDIRECT_KEY);
    return redirect || '/';
  }
  if (!Session.getToken()) {
    sessionStorage.setItem(LOGIN_REDIRECT_KEY, to.fullPath);
    return '/login';
  }
  if (to.path === '/home') {
    const redirect = sessionStorage.getItem(LOGIN_REDIRECT_KEY);
    sessionStorage.removeItem(LOGIN_REDIRECT_KEY);
    return redirect || '/';
  }
  return true;
});

export default router;
