import { createRouter, createWebHashHistory } from 'vue-router';
import type { Component } from 'vue';
import { Session } from '@mango/common/utils/storage';
import { getMangoAdminShellOptions } from './config';

export function createMangoAdminRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      {
        path: '/login',
        name: 'Login',
        component: resolveLoginComponent,
      },
      {
        path: '/',
        name: 'Shell',
        component: resolveShellComponent,
        meta: { isHide: true },
      },
      {
        path: '/:pathMatch(.*)*',
        name: 'ShellMenu',
        component: resolveShellComponent,
        meta: { isHide: true },
      },
    ],
  });
}

function resolveLoginComponent(): Promise<Component> {
  const login = getMangoAdminShellOptions().components?.login;
  return login ? Promise.resolve(login) : import('@mango/auth').then(m => m.LoginView);
}

function resolveShellComponent(): Promise<Component> {
  const shell = getMangoAdminShellOptions().components?.shell;
  return shell ? Promise.resolve(shell) : import('./ShellView.vue').then(m => m.default);
}

export const router = createMangoAdminRouter();

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
