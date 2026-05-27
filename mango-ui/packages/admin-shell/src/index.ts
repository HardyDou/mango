import { createApp, type App as VueApp } from 'vue';
import type { Router } from 'vue-router';
import { registerUnauthorizedHandler, setRequestBaseUrl } from '@mango/common/utils/request';
import { Session } from '@mango/common/utils/storage';
import 'element-plus/dist/index.css';
import '@mango/common/theme/index.scss';
import App from './App.vue';
import { installShellApp } from './appBootstrap';
import { configureMangoAdminShell, getMangoAdminShellOptions, type MangoAdminShellOptions } from './config';
import { createMangoAdminRouter } from './router';
import { onShellRuntimeUnauthorized } from './runtime/runtimeHost';

export interface MangoAdminAppInstance {
  app: VueApp;
  router: Router;
  mount: (target?: string | Element) => Element | undefined;
}

export function createMangoAdminApp(options: MangoAdminShellOptions = {}): MangoAdminAppInstance {
  const resolvedOptions = configureMangoAdminShell(options);
  const apiBaseUrl = resolvedOptions.apiBaseUrl || '/api';
  setRequestBaseUrl(apiBaseUrl);

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
export { createMangoAdminRouter } from './router';
export { getShellPinia, installShellApp } from './appBootstrap';
export { configureMangoAdminShell, getMangoAdminShellOptions };
export type { MangoAdminShellOptions };
export * from './runtime/menuHost';
export * from './runtime/runtimeConfig';
export * from './runtime/runtimeHost';
