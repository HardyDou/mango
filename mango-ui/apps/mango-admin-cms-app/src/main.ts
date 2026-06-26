import type { App as VueApp } from 'vue';
import { createPinia } from 'pinia';
import { createI18n } from 'vue-i18n';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { registerUnauthorizedHandler } from '@mango/common';
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

declare global {
  interface Window {
    $wujie?: {
      props?: {
        mangoRuntime?: import('@mango/app-runtime').MangoAppRuntime;
      };
    };
  }
}

function installCommon(appInstance: VueApp) {
  registerDefaultAdminPages({ features: ['cms', 'file'] });
  registerMangoFileAdminPages();
  registerMangoCmsAdminPages();
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    appInstance.component(key, component);
  }
  appInstance.use(ElementPlus);
  appInstance.use(createPinia());
  appInstance.use(createI18n({
    legacy: false,
    locale: 'zh-cn',
    fallbackLocale: 'zh-cn',
    messages: { 'zh-cn': {} },
  }));
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
      window.$wujie?.props?.mangoRuntime?.eventBus.emit('unauthorized');
    });
    return unbindTheme;
  },
});
