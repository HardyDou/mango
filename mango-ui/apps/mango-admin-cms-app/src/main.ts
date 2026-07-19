import type { App as VueApp } from 'vue';
import { createPinia } from 'pinia';
import { createI18n } from 'vue-i18n';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { createMangoHttpClient, registerUnauthorizedHandler, Session } from '@mango/common';
import { MANGO_HTTP_CLIENT_KEY, type MangoAppRuntime } from '@mango/app-runtime';
import { registerDefaultAdminPages } from '@mango/admin-pages';
import { registerMangoFileAdminPages } from '@mango/file/admin-pages';
import { registerMangoCmsAdminPages } from '@mango/cms/admin-pages';
import { bindMangoRuntimeTheme, createMangoWujieVueApp } from '@mango/app-runtime/vue-micro';
import 'element-plus/dist/index.css';
import '@mango/common/theme/index.scss';
import '@mango/common/style.css';
import '@mango/file/style.css';
import '@mango/cms/style.css';
import RuntimeRoot from './App.vue';
import router from './router';

let standaloneHttpClient: ReturnType<typeof createMangoHttpClient> | undefined;

function getStandaloneHttpClient() {
  standaloneHttpClient ||= createMangoHttpClient({
    baseUrl: '/api',
    getAccessToken: () => Session.getToken?.() || '',
    getTenantId: () => (Session.get('userInfo') || {}).tenantId ?? Session.get('tenantId'),
    onUnauthorized: () => router.push('/login'),
  });
  return standaloneHttpClient;
}

function installCommon(appInstance: VueApp, runtime?: MangoAppRuntime) {
  registerDefaultAdminPages({ features: ['cms', 'file'] });
  registerMangoFileAdminPages();
  registerMangoCmsAdminPages();
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    appInstance.component(key, component);
  }
  appInstance.use(ElementPlus);
  appInstance.use(createPinia());
  appInstance.use(
    createI18n({
      legacy: false,
      locale: 'zh-cn',
      fallbackLocale: 'zh-cn',
      messages: { 'zh-cn': {} },
    }),
  );
  appInstance.provide(MANGO_HTTP_CLIENT_KEY, runtime?.httpClient || getStandaloneHttpClient());
}

createMangoWujieVueApp({
  standaloneRoot: () => import('./StandaloneRoot.vue'),
  standaloneRouter: router,
  runtimeRoot: RuntimeRoot,
  install: installCommon,
  onStandaloneReady() {
    registerUnauthorizedHandler(async () => {
      await router.push('/login');
    });
  },
  onMicroReady(runtime) {
    const unbindTheme = bindMangoRuntimeTheme(runtime);
    registerUnauthorizedHandler(async () => {
      runtime?.eventBus.emit('unauthorized');
    });
    return unbindTheme;
  },
});
