import { createApp, type App as VueApp } from 'vue';
import { createPinia } from 'pinia';
import { createMemoryHistory, createRouter } from 'vue-router';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { registerUnauthorizedHandler } from '@mango/common';
import { registerDefaultAdminPages } from '@mango/admin-pages';
import type { MangoAppRuntime } from '@mango/app-runtime';
import 'element-plus/dist/index.css';
import '@mango/common/theme/index.scss';
import StandaloneRoot from './StandaloneRoot.vue';
import RuntimeRoot from './App.vue';
import router from './router';

let app: VueApp | undefined;

declare global {
  interface Window {
    __POWERED_BY_WUJIE__?: boolean;
    __WUJIE_MOUNT?: () => void;
    __WUJIE_UNMOUNT?: () => void;
    $wujie?: {
      props?: {
        mangoRuntime?: MangoAppRuntime;
      };
    };
  }
}

function installCommon(appInstance: VueApp) {
  registerDefaultAdminPages();
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    appInstance.component(key, component);
  }
  appInstance.use(ElementPlus);
  appInstance.use(createPinia());
  appInstance.config.globalProperties.$t = (key: string) => ({
    'login.title': 'Mango 管理平台',
    'login.username.placeholder': '用户名',
    'login.password.placeholder': '密码',
    'login.btn': '登 录',
  }[key] || key);
}

function mountStandalone() {
  app = createApp(StandaloneRoot);
  installCommon(app);
  app.use(router);
  registerUnauthorizedHandler(async () => {
    await router.push('/login');
  });
  app.mount('#app');
}

function mountWujie() {
  const runtime = window.$wujie?.props?.mangoRuntime;
  app = createApp(RuntimeRoot);
  installCommon(app);
  const runtimeRouter = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: '/:pathMatch(.*)*',
        component: { render: () => null },
      },
    ],
  });
  app.use(runtimeRouter);
  if (runtime) {
    app.provide('mangoRuntime', runtime);
    void runtimeRouter.push(runtime.menu?.path || '/');
  }
  registerUnauthorizedHandler(async () => {
    (window.$wujie?.props?.mangoRuntime?.eventBus as any)?.emit?.('unauthorized');
  });
  app.mount('#app');
}

function unmountWujie() {
  app?.unmount();
  app = undefined;
}

if (window.__POWERED_BY_WUJIE__) {
  window.__WUJIE_MOUNT = mountWujie;
  window.__WUJIE_UNMOUNT = unmountWujie;
} else {
  mountStandalone();
}
