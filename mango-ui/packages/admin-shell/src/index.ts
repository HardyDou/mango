import { createApp, type App as VueApp } from 'vue';
import type { Router } from 'vue-router';
import { registerUnauthorizedHandler, setRequestBaseUrl } from '@mango/common/utils/request';
import { Session } from '@mango/common/utils/storage';
import 'element-plus/dist/index.css';
import '@mango/common/theme/index.css';
import App from './App.vue';
import { installShellApp } from './appBootstrap';
import { configureMangoAdminShell, getMangoAdminShellOptions, type MangoAdminShellOptions } from './config';
import { createMangoAdminRouter } from './router';
import { ensureFeatureRegistrars } from './runtime/featureRegistrars';
import { onShellRuntimeUnauthorized } from './runtime/runtimeHost';

export interface MangoAdminAppInstance {
  app: VueApp;
  router: Router;
  mount: (target?: string | Element) => Element | undefined;
}

export function createMangoAdminApp(options: MangoAdminShellOptions = {}): MangoAdminAppInstance {
  const resolvedOptions = configureMangoAdminShell({
    contentMode: 'runtime-outlet',
    ...options,
  });
  const apiBaseUrl = resolvedOptions.apiBaseUrl || '/api';
  setRequestBaseUrl(apiBaseUrl);
  void ensureFeatureRegistrars().catch(error => {
    console.error('[mango-shell] failed to register shell features', error);
  });

  const app = createApp(App);
  const router = createMangoAdminRouter();

  installShellApp(app, resolvedOptions);
  app.use(router);

  async function redirectToLogin() {
    Session.clearSession();
    await router.push('/login');
  }

  registerUnauthorizedHandler(redirectToLogin);
  onShellRuntimeUnauthorized(redirectToLogin);

  return {
    app,
    router,
    mount(target = getMangoAdminShellOptions().mountTarget || '#app') {
      return app.mount(target);
    },
  };
}

export { default as MangoAdminShellApp } from './App.vue';
export { default as MangoAdminShellView } from './ShellView.vue';
export { default as MangoAdminLayout } from './layout/index.vue';
export { default as MangoAdminParentView } from './layout/routerView/parent.vue';
export { createMangoAdminRouter } from './router';
export { getShellPinia, installShellApp } from './appBootstrap';
export { configureMangoAdminShell, getMangoAdminShellOptions };
export { ensureFeatureRegistrars };
export type { MangoAdminFeatureRegistrar, MangoAdminFeatureRegistration, MangoAdminShellOptions };
export * from './stores';
export * from './runtime/menuHost';
export * from './runtime/runtimeConfig';
export * from './runtime/runtimeHost';
export * from './runtime/homeWidgets';
