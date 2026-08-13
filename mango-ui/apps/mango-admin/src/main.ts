import { createApp } from 'vue';
import { createPinia } from 'pinia';
import piniaPluginPersist from 'pinia-plugin-persistedstate';
import { registerUnauthorizedHandler, Session } from '@mango/common';
import { MANGO_HTTP_CLIENT_KEY } from '@mango/app-runtime';
import { createMangoHttpClient } from '@mango/http-client';
import { installMangoAuth } from '@mango/auth';
import {
  configureMangoAdminShell,
  getMangoAdminAuthProfileSlots,
  installAdminBrandingRuntime,
  reportUnhandledError,
} from '@mango/admin-shell';
import { mangoFullAdminFeatureRegistrars } from '@mango/admin/full';
import { systemQuickEntryWidgets, systemUserProfileWidgets } from '@mango/system';
import App from './App.vue';
import router from './router';
import { i18n } from './i18n';
import ElementPlus from 'element-plus';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import '@mango/admin/style-full.css';
import { initThemeBeforeRender } from '@/utils/themeInit';
import { registerAuthDirectives } from './directive/authDirective';

configureMangoAdminShell({
  features: 'full',
  featureRegistrars: mangoFullAdminFeatureRegistrars,
  widgets: [...systemUserProfileWidgets, ...systemQuickEntryWidgets],
});

// MSW Mock 支持（开发环境且启用时）
async function enableMock() {
  if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true') {
    const { startMockWorker } = await import('./mocks/browser');
    await startMockWorker();
    return;
  }

  if ('serviceWorker' in navigator) {
    const registrations = await navigator.serviceWorker.getRegistrations();
    await Promise.all(
      registrations
        .filter((registration) => registration.active?.scriptURL.includes('/mockServiceWorker.js'))
        .map((registration) => registration.unregister()),
    );
  }
}

const app = createApp(App);
const adminHttpClient = createMangoHttpClient({
  baseUrl: window.location.origin + '/api',
  getAccessToken: () => Session.getToken?.() || '',
  getTenantId: () => (Session.get('userInfo') || {}).tenantId || Session.get('tenantId'),
  onUnauthorized: () => redirectToLogin(),
});
app.provide(MANGO_HTTP_CLIENT_KEY, adminHttpClient);
app.onUnmount(() => adminHttpClient.destroy());

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue 错误:', err);
  console.error('组件:', instance);
  console.error('错误信息:', info);
  reportUnhandledError(err);
};

const handleUnhandledRejection = (event: PromiseRejectionEvent) => {
  console.error('Unhandled promise rejection:', event.reason);
  event.preventDefault();
  reportUnhandledError(event.reason);
};
window.addEventListener('unhandledrejection', handleUnhandledRejection);
app.onUnmount(() => window.removeEventListener('unhandledrejection', handleUnhandledRejection));

app.use(createPinia().use(piniaPluginPersist));
app.use(router);
app.use(i18n);
app.use(ElementPlus, { locale: zhCn });
installMangoAuth(app, {
  login: {
    brand: {
      title: 'Mango Admin',
      subtitle: '企业级管理平台',
    },
    defaults: {
      tenantCode: 'default',
      realm: 'INTERNAL',
      actorType: 'INTERNAL_USER',
      partyType: 'INTERNAL_ORG',
      appCode: 'internal-admin',
      redirectPath: '/home',
    },
  },
  profile: {
    roleLabel: '超级管理员',
    slots: getMangoAdminAuthProfileSlots(),
  },
  password: {
    minLength: 6,
  },
});
installAdminBrandingRuntime();

// 在 Vue 首帧渲染前同步恢复 store 状态（布局/深色模式等），避免闪屏
initThemeBeforeRender();

// 注册权限指令
registerAuthDirectives(app);

async function redirectToLogin() {
  const currentRoute = router.currentRoute.value;
  await router.push(
    currentRoute.path === '/login'
      ? { path: '/login' }
      : { path: '/login', query: { redirect: currentRoute.fullPath } },
  );
}

registerUnauthorizedHandler(redirectToLogin);

// 启用 Mock（如果配置了）
await enableMock();

app.mount('#app');
