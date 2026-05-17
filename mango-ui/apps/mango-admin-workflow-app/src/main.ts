import type { App as VueApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { registerUnauthorizedHandler } from '@mango/common';
import { registerDefaultAdminPages } from '@mango/admin-pages';
import { createMangoWujieVueApp } from '@mango/app-runtime/vue-micro';
import 'element-plus/dist/index.css';
import '@mango/common/theme/index.scss';
import StandaloneRoot from './StandaloneRoot.vue';
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

createMangoWujieVueApp({
  standaloneRoot: StandaloneRoot,
  standaloneRouter: router,
  runtimeRoot: RuntimeRoot,
  install: installCommon,
  onStandaloneReady() {
    registerUnauthorizedHandler(async () => {
      await router.push('/login');
    });
  },
  onMicroReady() {
    registerUnauthorizedHandler(async () => {
      (window.$wujie?.props?.mangoRuntime?.eventBus as any)?.emit?.('unauthorized');
    });
  },
});
