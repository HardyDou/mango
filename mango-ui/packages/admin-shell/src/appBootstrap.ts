import type { App as VueApp } from 'vue';
import { createPinia, type Pinia } from 'pinia';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { createI18n, type I18n } from 'vue-i18n';
import { authAll, auths, canShowButton, type AuthRuleBindingValue } from '@mango/common/utils/authFunction';
import { mangoMessage } from '@mango/common/utils/message';
import { installMangoAuth } from '@mango/auth';
import type { MangoAdminShellOptions } from './config';
import { getMangoAdminShellOptions } from './config';

let shellPinia: Pinia | undefined;
let shellI18n: I18n | undefined;

export function getShellPinia() {
  if (!shellPinia) {
    shellPinia = createPinia();
  }
  return shellPinia;
}

export function getShellI18n() {
  if (!shellI18n) {
    shellI18n = createI18n({
      legacy: false,
      locale: 'zh-cn',
      fallbackLocale: 'zh-cn',
      messages: {
        'zh-cn': {
          login: {
            title: 'Mango 管理平台',
            username: { placeholder: '用户名' },
            password: { placeholder: '密码' },
            btn: '登 录',
          },
          orgSelector: {
            placeholder: '请选择组织',
            title: '选择组织',
            noData: '暂无组织数据',
            loading: '加载中',
            cancel: '取消',
            confirm: '确定',
          },
          chinaArea: {
            placeholder: '请选择省市区',
            noMatch: '无匹配数据',
            noData: '暂无区域数据',
          },
          sign: {
            placeholder: '请在此处签名',
            clear: '清空',
            confirm: '确定',
          },
          chat: {
            welcome: '您好，请输入消息',
            placeholder: '请输入消息',
          },
          websocket: {
            connect: '连接',
            disconnect: '断开',
          },
          sse: {
            connect: '连接',
            disconnect: '断开',
          },
        },
      },
    });
  }
  return shellI18n;
}

export function installShellApp(app: VueApp, options: MangoAdminShellOptions = getMangoAdminShellOptions()) {
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
  }
  installAuthDirectives(app);
  app.use(ElementPlus);
  app.use(getShellPinia());
  app.use(getShellI18n());
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

function installAuthDirectives(app: VueApp) {
  const applyVisible = (el: HTMLElement, visible: boolean) => {
    el.style.display = visible ? '' : 'none';
  };

  const resolveAuthVisible = (value: unknown) => {
    if (!value) {
      return true;
    }
    if (Array.isArray(value)) {
      return auths(value);
    }
    return canShowButton(value as string | AuthRuleBindingValue);
  };

  app.directive('auth', {
    mounted(el, binding) {
      applyVisible(el, resolveAuthVisible(binding.value));
    },
    updated(el, binding) {
      applyVisible(el, resolveAuthVisible(binding.value));
    },
  });
  app.directive('auths', {
    mounted(el, binding) {
      applyVisible(el, auths(Array.isArray(binding.value) ? binding.value : []));
    },
    updated(el, binding) {
      applyVisible(el, auths(Array.isArray(binding.value) ? binding.value : []));
    },
  });
  app.directive('auth-all', {
    mounted(el, binding) {
      applyVisible(el, authAll(Array.isArray(binding.value) ? binding.value : []));
    },
    updated(el, binding) {
      applyVisible(el, authAll(Array.isArray(binding.value) ? binding.value : []));
    },
  });
}
