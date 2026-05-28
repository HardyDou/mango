import type { App as VueApp } from 'vue';
import { createPinia, type Pinia } from 'pinia';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { mangoMessage } from '@mango/common';
import { installMangoAuth } from '@mango/auth';
import type { MangoAdminShellOptions } from './config';
import { getMangoAdminShellOptions } from './config';

let shellPinia: Pinia | undefined;

export function getShellPinia() {
  if (!shellPinia) {
    shellPinia = createPinia();
  }
  return shellPinia;
}

export function installShellApp(app: VueApp, options: MangoAdminShellOptions = getMangoAdminShellOptions()) {
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
  }
  app.use(ElementPlus);
  app.use(getShellPinia());
  installMangoAuth(app, {
    login: {
      brand: {
        title: options.title || 'Mango Admin',
        subtitle: '企业级管理平台',
      },
      ...options.login,
      defaults: {
        tenantCode: 'default',
        realm: 'INTERNAL',
        actorType: 'INTERNAL_USER',
        partyType: 'INTERNAL_ORG',
        appCode: 'internal-admin',
        redirectPath: '/home',
        ...options.login?.defaults,
      },
    },
    profile: {
      roleLabel: '超级管理员',
    },
    password: {
      minLength: 6,
    },
  });
  app.config.globalProperties.$t = (key: string) => ({
    'login.title': options.title || 'Mango 管理平台',
    'login.username.placeholder': '用户名',
    'login.password.placeholder': '密码',
    'login.btn': '登 录',
  }[key] || key);
  app.config.errorHandler = (err, instance, info) => {
    console.error('[mango-shell] Vue error:', err);
    console.error('[mango-shell] component:', instance);
    console.error('[mango-shell] info:', info);
    if (err && typeof err === 'object' && 'response' in err) {
      return;
    }
    mangoMessage.error('系统错误，请刷新页面');
  };
}
