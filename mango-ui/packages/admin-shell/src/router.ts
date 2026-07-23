import { createRouter, createWebHashHistory } from 'vue-router';
import { Session } from '@mango/common/utils/storage';
import { getShellPinia } from './appBootstrap';
import { getMangoAdminShellOptions } from './config';
import { useTagsViewRoutes } from './stores/tagsViewRoutes';

const MODULE_DIAGNOSTIC_FRAGMENT_PREFIX = '#mango-module-doctor=';

function captureModuleDiagnosticFragmentBeforeHashRouter() {
  if (typeof window === 'undefined' || !window.location.hash.startsWith(MODULE_DIAGNOSTIC_FRAGMENT_PREFIX)) {
    return;
  }
  const diagnosticWindow = window as Window & {
    __MANGO_PENDING_MODULE_DIAGNOSTIC_FRAGMENT__?: string;
  };
  const fragment = window.location.hash;
  diagnosticWindow.__MANGO_PENDING_MODULE_DIAGNOSTIC_FRAGMENT__ = fragment;
  window.history.replaceState(null, '', `${window.location.pathname}${window.location.search}`);
  queueMicrotask(() => {
    if (diagnosticWindow.__MANGO_PENDING_MODULE_DIAGNOSTIC_FRAGMENT__ === fragment) {
      delete diagnosticWindow.__MANGO_PENDING_MODULE_DIAGNOSTIC_FRAGMENT__;
    }
  });
}

captureModuleDiagnosticFragmentBeforeHashRouter();

function resolveLoginRouteComponent() {
  return getMangoAdminShellOptions().login?.component || (() => import('@mango/auth').then((m) => m.LoginView));
}

export function createMangoAdminRouter() {
  const router = createRouter({
    history: createWebHashHistory(),
    routes: [
      {
        path: '/login',
        name: 'Login',
        component: resolveLoginRouteComponent(),
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
      return {
        path: '/login',
        query: { redirect: to.fullPath },
      };
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
