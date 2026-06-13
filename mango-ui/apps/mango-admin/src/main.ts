import { createApp } from 'vue';
import { createPinia } from 'pinia';
import piniaPluginPersist from 'pinia-plugin-persistedstate';
import { mangoMessage, registerUnauthorizedHandler } from '@mango/common';
import { installMangoAuth } from '@mango/auth';
import { configureMangoAdminShell } from '@mango/admin-shell';
import App from './App.vue';
import router from './router';
import { i18n } from './i18n';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import '@mango/admin/style.css';
import '@mango/payment/style.css';
import { initThemeBeforeRender } from '@/utils/themeInit';
import { registerAuthDirectives } from './directive/authDirective';

configureMangoAdminShell({
  features: 'full',
});

// MSW Mock 支持（开发环境且启用时）
async function enableMock() {
  if (import.meta.env.VITE_USE_MOCK === 'true') {
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

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

// 全局错误处理
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue 错误:', err);
  console.error('组件:', instance);
  console.error('错误信息:', info);
  if (err && typeof err === 'object' && 'response' in err) {
    return;
  }
  mangoMessage.error('系统错误，请刷新页面');
};

app.use(createPinia().use(piniaPluginPersist));
app.use(router);
app.use(i18n);
app.use(ElementPlus);
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
  },
  password: {
    minLength: 6,
  },
});

// 在 Vue 首帧渲染前同步恢复 store 状态（布局/深色模式等），避免闪屏
initThemeBeforeRender();

// 注册权限指令
registerAuthDirectives(app);

registerUnauthorizedHandler(async () => {
  await router.push('/login');
});

// 启用 Mock（如果配置了）
await enableMock();

app.mount('#app');
