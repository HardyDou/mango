import type { App as VueApp } from 'vue';
import { createPinia, type Pinia } from 'pinia';
import ElementPlus from 'element-plus';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import { mangoMessage } from '@mango/common/utils/message';
import { installMangoAuth } from '@mango/auth';
import type { MangoAdminShellOptions } from './config';
import { getMangoAdminShellOptions } from './config';
import { useLayoutStore } from './stores/layout';
import { usePreferencesStore } from './stores/preferences';
import { useThemeStore } from './stores/theme';

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
  const pinia = getShellPinia();
  app.use(pinia);
  applyShellOptionsToStores(options);
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
      ...options.profile,
    },
    password: {
      minLength: 6,
      ...options.password,
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

function applyShellOptionsToStores(options: MangoAdminShellOptions) {
  const themeStore = useThemeStore(getShellPinia());
  const layoutStore = useLayoutStore(getShellPinia());
  const preferencesStore = usePreferencesStore(getShellPinia());
  const theme = options.theme;
  const layout = options.layout;

  if (theme?.primary) themeStore.primary = theme.primary;
  if (typeof theme?.isDark === 'boolean') themeStore.isDark = theme.isDark;
  if (theme?.topBar) themeStore.topBar = theme.topBar;
  if (theme?.topBarColor) themeStore.topBarColor = theme.topBarColor;
  if (theme?.menuBar) themeStore.menuBar = theme.menuBar;
  if (theme?.menuBarColor) themeStore.menuBarColor = theme.menuBarColor;
  if (theme?.menuBarActiveColor) themeStore.menuBarActiveColor = theme.menuBarActiveColor;
  if (theme?.columnsMenuBar) themeStore.columnsMenuBar = theme.columnsMenuBar;
  if (theme?.columnsMenuBarColor) themeStore.columnsMenuBarColor = theme.columnsMenuBarColor;
  if (theme?.componentSize) preferencesStore.globalComponentSize = theme.componentSize;

  if (layout?.defaultLayout) layoutStore.layout = layout.defaultLayout;
  if (typeof layout?.showLogo === 'boolean') layoutStore.isShowLogo = layout.showLogo;
  if (typeof layout?.showBreadcrumb === 'boolean') layoutStore.isBreadcrumb = layout.showBreadcrumb;
  if (typeof layout?.showTagsView === 'boolean') layoutStore.isTagsview = layout.showTagsView;
}
