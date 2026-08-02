import { createRouter, createWebHashHistory } from 'vue-router';
import { Session } from '@mango/common';
import { captureProviderCallbackBeforeHashRouter } from '@mango/auth';

export const LOGIN_REDIRECT_KEY = 'MANGO_RBAC_LOGIN_REDIRECT';

captureProviderCallbackBeforeHashRouter();

export const router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'RbacStandalone',
      component: () => import('./StandaloneRoot.vue'),
    },
    {
      path: '/home',
      name: 'RbacLoginLanding',
      component: () => import('./StandaloneRoot.vue'),
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@mango/auth').then((m) => m.LoginView),
    },
    {
      path: '/provider-callback',
      name: 'ProviderCallback',
      component: () => import('@mango/auth').then((m) => m.ProviderCallbackView),
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'RbacStandaloneMenu',
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
  if (to.path === '/provider-callback') {
    return true;
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
