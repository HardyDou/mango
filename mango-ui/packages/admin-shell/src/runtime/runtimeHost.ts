import { computed, createApp, h, nextTick, ref, type App as VueApp, type Component, type Ref } from 'vue';
import type { Router } from 'vue-router';
import { del, get, post, put, type RequestConfig } from '@mango/common/utils/request';
import { Session } from '@mango/common/utils/storage';
import {
  createRuntimeEventBus,
  emitMangoRuntimeLog,
  loadRuntimeConfig,
  preloadMicroApp,
  resolveAdapter,
  type MangoAppRuntime,
  type MangoRuntimeTheme,
  type MangoModuleRuntimeConfig,
  type MangoRuntimeConfig,
  type MangoRuntimeConfigDiagnostic,
  MangoRuntimeConfigError,
  type MangoRuntimeAppConfig,
} from '@mango/app-runtime';
import { getPageLoader } from '@mango/admin-pages/core';
import { useThemeStore } from '../stores/theme';
import { useLayoutStore } from '../stores/layout';
import { usePreferencesStore } from '../stores/preferences';
import { installShellApp } from '../appBootstrap';
import { getMangoAdminShellOptions } from '../config';
import type { ShellMenu, ShellRouteMenu } from './menuHost';
import { defaultRuntimeConfig, loadShellRuntimeConfig } from './runtimeConfig';

const shellRuntimeEventBus = createRuntimeEventBus();
let lastRuntimeDecision: RuntimeDecision | undefined;
let lastRuntimeConfigDiagnostics: MangoRuntimeConfigDiagnostic[] = [];

export interface RuntimeDecision {
  menuName?: string;
  path?: string;
  component?: string;
  moduleCode?: string;
  menuPageType?: string;
  resolvedPageType: string;
  runtimeMode?: string;
  runtimeCode?: string;
  entry?: string;
  decidedAt: string;
}

export function useRuntimeHost(containerRef: Ref<HTMLElement | undefined>, router: Router) {
  const loading = ref(false);
  const runtimeApps = ref<MangoRuntimeAppConfig[]>([]);
  const runtimeConfig = ref<MangoRuntimeConfig>(defaultRuntimeConfig);
  const activeRuntimeApp = ref<MangoRuntimeAppConfig>();
  const runtimeDecision = ref<RuntimeDecision>();
  const runtimeConfigAvailable = ref(true);
  let mountedLocalPage: VueApp | undefined;
  let mountedMicroConfig: MangoRuntimeAppConfig | undefined;
  let currentMenu: ShellMenu | undefined;
  let mountSeq = 0;
  let defaultPagesPromise: Promise<void> | undefined;

  async function loadRuntimeApps() {
    loading.value = true;
    try {
      runtimeConfig.value = await loadShellRuntimeConfig();
      runtimeConfigAvailable.value = true;
      recordRuntimeConfigDiagnostics(runtimeConfig.value.diagnostics);
      runtimeApps.value = toRuntimeApps(runtimeConfig.value);
      preloadRuntimeApps(runtimeApps.value);
      return true;
    } catch (error) {
      runtimeApps.value = [];
      runtimeConfig.value = defaultRuntimeConfig;
      runtimeConfigAvailable.value = false;
      activeRuntimeApp.value = undefined;
      if (isMangoRuntimeConfigError(error)) {
        recordRuntimeConfigDiagnostics(error.diagnostics);
      }
      await mountFallback();
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function mountMenu(menu?: ShellMenu | ShellRouteMenu) {
    await nextTick();
    if (!menu) {
      return;
    }
    const seq = ++mountSeq;
    const sourceMenu = normalizeMenu(menu);
    currentMenu = sourceMenu;

    const container = containerRef.value;
    if (!container) {
      return;
    }

    await unmountCurrentPage();
    if (!isLatestMount(seq)) {
      return;
    }
    container.innerHTML = '';
    if (!runtimeConfigAvailable.value) {
      await mountFallback();
      return;
    }

    const moduleConfig = resolveModuleConfig(sourceMenu);
    const pageType = resolvePageType(sourceMenu, moduleConfig);
    runtimeDecision.value = createRuntimeDecision(sourceMenu, moduleConfig, pageType);
    recordRuntimeDecision(runtimeDecision.value);
    applyRuntimeMarker(container, runtimeDecision.value);
    if (pageType === 'IFRAME') {
      if (!isLatestMount(seq)) {
        return;
      }
      mountIframe(sourceMenu);
      return;
    }
    if (pageType === 'EXTERNAL_LINK') {
      if (sourceMenu.externalUrl) {
        window.open(sourceMenu.externalUrl, '_blank', 'noopener,noreferrer');
      }
      return;
    }
    if (pageType === 'MICRO_ROUTE') {
      try {
        await mountMicroMenu(sourceMenu, moduleConfig, seq);
      } catch (error) {
        if (isLatestMount(seq)) {
          mountRuntimeError(sourceMenu, error, moduleConfig);
        }
      }
      return;
    }
    try {
      await mountLocalMenu(sourceMenu, seq);
    } catch (error) {
      if (isLatestMount(seq)) {
        mountRuntimeError(sourceMenu, error, moduleConfig);
      }
    }
  }

  async function mountLocalMenu(menu: ShellMenu, seq: number) {
    const container = containerRef.value;
    if (!container) {
      return;
    }

    await ensureDefaultPages();
    if (!isLatestMount(seq)) {
      return;
    }
    const loader = getPageLoader(menu.moduleCode, menu.component) || getPageLoader(undefined, menu.component);
    if (!loader) {
      await mountNotFound(seq);
      return;
    }

    const module = await loader();
    if (!isLatestMount(seq)) {
      return;
    }
    const component = resolveLoadedComponent(module);
    mountedLocalPage = createApp({ render: () => h(component) });
    installShellApp(mountedLocalPage);
    mountedLocalPage.use(router);
    mountedLocalPage.mount(container);
  }

  async function mountMicroMenu(menu: ShellMenu, moduleConfig: MangoModuleRuntimeConfig | undefined, seq: number) {
    const config = resolveRuntimeConfig(menu, moduleConfig);
    const container = containerRef.value;
    if (!config || !container) {
      mountRuntimeConfigError(menu, moduleConfig);
      return;
    }
    activeRuntimeApp.value = config;
    mountedMicroConfig = config;
    const adapter = resolveAdapter(config.appType || 'MICRO_APP');
    await adapter.mount(config, container, createRuntime(config, menu));
    if (!isLatestMount(seq)) {
      await adapter.unmount?.(config);
    }
  }

  async function mountNotFound(seq: number) {
    await ensureDefaultPages();
    if (!isLatestMount(seq)) {
      return;
    }
    const configuredNotFound = getMangoAdminShellOptions().components?.notFound;
    if (configuredNotFound) {
      mountedLocalPage = createApp({ render: () => h(configuredNotFound) });
      installShellApp(mountedLocalPage);
      mountedLocalPage.use(router);
      mountedLocalPage.mount(containerRef.value!);
      return;
    }
    const loader = getPageLoader('mango-shell', 'error/404') || getPageLoader(undefined, 'error/404');
    if (!loader) {
      mountMessage('404');
      return;
    }
    const module = await loader();
    if (!isLatestMount(seq)) {
      return;
    }
    const component = resolveLoadedComponent(module);
    mountedLocalPage = createApp({ render: () => h(component) });
    installShellApp(mountedLocalPage);
    mountedLocalPage.use(router);
    mountedLocalPage.mount(containerRef.value!);
  }

  async function retryCurrentMenu() {
    if (!currentMenu) {
      return;
    }
    await mountMenu(currentMenu);
  }

  function mountIframe(menu: ShellMenu) {
    const container = containerRef.value;
    const url = menu.externalUrl;
    if (!container || !url) {
      mountMessage('缺少 iframe 地址');
      return;
    }
    const iframe = document.createElement('iframe');
    iframe.src = url;
    iframe.style.width = '100%';
    iframe.style.minHeight = 'calc(100vh - 160px)';
    iframe.style.border = '0';
    container.appendChild(iframe);
  }

  function resolveRuntimeConfig(menu: ShellMenu, moduleConfig?: MangoModuleRuntimeConfig) {
    const runtimeCode = resolveRuntimeCode(menu, moduleConfig);
    if (!runtimeCode) {
      return undefined;
    }
    return runtimeApps.value.find(app => app.appCode === runtimeCode);
  }

  function ensureDefaultPages() {
    if (!defaultPagesPromise) {
      defaultPagesPromise = import('@mango/admin-pages/defaults').then(({ registerDefaultAdminPages }) => {
        registerDefaultAdminPages({
          shellPages: {
            notFound: () => import('../views/error/404.vue'),
          },
        });
      });
    }
    return defaultPagesPromise;
  }

  function resolveModuleConfig(menu: ShellMenu) {
    return menu.moduleCode ? runtimeConfig.value.modules[menu.moduleCode] : undefined;
  }

  function resolvePageType(menu: ShellMenu, moduleConfig?: MangoModuleRuntimeConfig) {
    if (menu.pageType === 'IFRAME' || menu.pageType === 'EXTERNAL_LINK' || menu.pageType === 'BUTTON') {
      return menu.pageType;
    }
    return moduleConfig?.mode === 'micro' ? 'MICRO_ROUTE' : 'LOCAL_ROUTE';
  }

  async function unmountCurrentPage() {
    mountedLocalPage?.unmount();
    mountedLocalPage = undefined;
    if (mountedMicroConfig) {
      await resolveAdapter(mountedMicroConfig.appType || 'MICRO_APP').unmount?.(mountedMicroConfig);
      mountedMicroConfig = undefined;
    }
  }

  function mountMessage(message: string) {
    const container = containerRef.value;
    if (!container) {
      return;
    }
    renderRuntimeState(container, {
      title: message,
      description: '请检查菜单运行类型、组件路径或运行配置。',
    });
  }

  function mountRuntimeError(menu: ShellMenu, error: unknown, moduleConfig?: MangoModuleRuntimeConfig) {
    const container = containerRef.value;
    if (!container) {
      return;
    }
    const errorMessage = error instanceof Error ? error.message : '页面加载失败';
    const entry = moduleConfig?.entry || activeRuntimeApp.value?.entryUrl || '';
    const runtimeCode = moduleConfig?.runtimeCode || activeRuntimeApp.value?.appCode || menu.moduleCode || '';
    renderRuntimeState(container, {
      title: '页面加载失败',
      description: `${menu.menuName || menu.path || '当前页面'}：${errorMessage}`,
      details: [
        runtimeCode ? `运行单元：${runtimeCode}` : '',
        entry ? `入口地址：${entry}` : '',
      ].filter(Boolean),
      retry: retryCurrentMenu,
    });
  }

  function mountRuntimeConfigError(menu: ShellMenu, moduleConfig?: MangoModuleRuntimeConfig) {
    const container = containerRef.value;
    if (!container) {
      return;
    }
    const runtimeCode = resolveRuntimeCode(menu, moduleConfig);
    const diagnostics = findRuntimeDiagnostics(menu, moduleConfig);
    renderRuntimeState(container, {
      title: `缺少微应用运行配置：${runtimeCode}`,
      details: diagnostics.length
        ? diagnostics.map(item => item.message)
        : ['请检查 runtime-config.json 是否配置 entry 和 runtimeCode。'],
      retry: retryCurrentMenu,
    });
  }

  function findRuntimeDiagnostics(menu: ShellMenu, moduleConfig?: MangoModuleRuntimeConfig): MangoRuntimeConfigDiagnostic[] {
    const moduleCode = menu.moduleCode;
    const runtimeCode = moduleConfig?.runtimeCode;
    return (runtimeConfig.value.diagnostics || []).filter((item: MangoRuntimeConfigDiagnostic) =>
      item.moduleCode === moduleCode || item.moduleCode === runtimeCode
    );
  }

  async function mountFallback() {
    await nextTick();
    const container = containerRef.value;
    if (!container) {
      return;
    }
    renderRuntimeState(container, {
      title: '运行配置加载失败',
      description: '请确认登录态、租户开通关系、runtime-config.json 和后端服务状态。',
    });
  }

  function dispose() {
    mountSeq += 1;
    return unmountCurrentPage();
  }

  function isLatestMount(seq: number) {
    return seq === mountSeq;
  }

  return {
    loading,
    runtimeApps,
    runtimeConfig,
    runtimeConfigAvailable: computed(() => runtimeConfigAvailable.value),
    activeRuntimeApp: computed(() => activeRuntimeApp.value),
    runtimeDecision: computed(() => runtimeDecision.value),
    loadRuntimeApps,
    mountMenu,
    dispose,
  };
}

function toRuntimeApps(config: MangoRuntimeConfig): MangoRuntimeAppConfig[] {
  return Object.entries(config.modules as Record<string, MangoModuleRuntimeConfig>)
    .filter(([, module]) => module.mode === 'micro' && module.entry)
    .map(([moduleCode, module]) => ({
      appCode: module.runtimeCode || moduleCode,
      appName: module.runtimeCode || moduleCode,
      appType: module.appType || 'MICRO_APP',
      deployMode: 'REMOTE',
      entryUrl: module.entry,
      styleUrl: module.style,
      framework: module.framework || 'vue3',
      sandboxEnabled: false,
      styleIsolation: 'NONE',
      status: 1,
      timeoutMs: module.timeoutMs || 15000,
      preload: module.preload === true,
      alive: module.alive === true,
    }));
}

function isMangoRuntimeConfigError(error: unknown): error is MangoRuntimeConfigError {
  return error instanceof MangoRuntimeConfigError;
}

function resolveRuntimeCode(menu: ShellMenu, moduleConfig?: MangoModuleRuntimeConfig) {
  if (moduleConfig?.runtimeCode) {
    return moduleConfig.runtimeCode;
  }
  if (menu.appCode && menu.appCode !== 'internal-admin') {
    return menu.appCode;
  }
  return menu.moduleCode || menu.path || menu.component || 'unknown';
}

function preloadRuntimeApps(apps: MangoRuntimeAppConfig[]) {
  apps
    .filter(app => app.preload)
    .forEach((app) => {
      try {
        preloadMicroApp(app, createBaseRuntime(app));
      } catch (error) {
        console.warn('[mango-runtime] preload failed', app.appCode, error);
      }
    });
}

function resolveLoadedComponent(module: unknown): Component {
  if (isComponentModule(module)) {
    return module.default;
  }
  return module as Component;
}

function isComponentModule(module: unknown): module is { default: Component } {
  return typeof module === 'object' && module !== null && 'default' in module;
}

function normalizeMenu(menu: ShellMenu | ShellRouteMenu): ShellMenu {
  return 'sourceMenu' in menu ? menu.sourceMenu : menu;
}

function renderRuntimeState(
  container: HTMLElement,
  options: {
    title: string;
    description?: string;
    details?: string[];
    retry?: () => void | Promise<void>;
  }
) {
  container.replaceChildren();
  const wrapper = document.createElement('div');
  wrapper.className = 'micro-runtime-empty';
  const content = document.createElement('div');
  const title = document.createElement('h3');
  title.textContent = options.title;
  content.appendChild(title);
  if (options.description) {
    const description = document.createElement('p');
    description.textContent = options.description;
    content.appendChild(description);
  }
  options.details?.forEach((detail) => {
    const item = document.createElement('p');
    item.className = 'micro-runtime-detail';
    item.textContent = detail;
    content.appendChild(item);
  });
  if (options.retry) {
    const retry = document.createElement('button');
    retry.className = 'micro-runtime-retry';
    retry.type = 'button';
    retry.textContent = '重试';
    retry.addEventListener('click', () => {
      void options.retry?.();
    }, { once: true });
    content.appendChild(retry);
  }
  wrapper.appendChild(content);
  container.appendChild(wrapper);
}

function createRuntime(config: MangoRuntimeAppConfig, menu?: ShellMenu): MangoAppRuntime {
  return {
    ...createBaseRuntime(config),
    menu,
  };
}

function createBaseRuntime(config: MangoRuntimeAppConfig): MangoAppRuntime {
  const userInfo = Session.get('userInfo') || {};
  return {
    token: Session.getToken?.() || '',
    tenantId: userInfo.tenantId,
    appCode: config.appCode,
    apiBaseUrl: resolveShellApiBaseUrl(),
    menu: undefined,
    userInfo,
    permissions: userInfo.permissions || [],
    request: {
      get: async <T = unknown>(url: string, config?: unknown) => get(url, config as RequestConfig | undefined) as Promise<T>,
      post: async <T = unknown>(url: string, data?: unknown, config?: unknown) => post(url, data, config as RequestConfig | undefined) as Promise<T>,
      put: async <T = unknown>(url: string, data?: unknown, config?: unknown) => put(url, data, config as RequestConfig | undefined) as Promise<T>,
      delete: async <T = unknown>(url: string, config?: unknown) => del(url, config as RequestConfig | undefined) as Promise<T>,
    },
    eventBus: shellRuntimeEventBus,
    theme: createShellRuntimeTheme(),
  };
}

export function createShellRuntimeTheme(): MangoRuntimeTheme {
  const themeStore = useThemeStore();
  const layoutStore = useLayoutStore();
  const preferencesStore = usePreferencesStore();
  return {
    primary: themeStore.primary,
    isDark: themeStore.isDark,
    topBar: themeStore.topBar,
    topBarColor: themeStore.topBarColor,
    menuBar: themeStore.menuBar,
    menuBarColor: themeStore.menuBarColor,
    menuBarActiveColor: themeStore.menuBarActiveColor,
    columnsMenuBar: themeStore.columnsMenuBar,
    columnsMenuBarColor: themeStore.columnsMenuBarColor,
    layout: layoutStore.layout,
    componentSize: preferencesStore.globalComponentSize,
    tokens: {
      '--mango-color-primary': themeStore.primary,
      '--el-color-primary': themeStore.primary,
      '--mango-bg-top-bar': themeStore.topBar,
      '--mango-bg-menu-bar': themeStore.menuBar,
      '--mango-bg-columns-menu-bar': themeStore.columnsMenuBar,
    },
  };
}

export function emitShellThemeChange(theme: MangoRuntimeTheme = createShellRuntimeTheme()) {
  shellRuntimeEventBus.emit('theme-change', theme);
  emitMangoRuntimeLog({
    level: 'info',
    event: 'theme-change',
    message: 'Shell runtime theme changed',
    detail: {
      primary: theme.primary,
      isDark: theme.isDark,
      layout: theme.layout,
      componentSize: theme.componentSize,
    },
  });
}

export function onShellRuntimeUnauthorized(handler: () => void | Promise<void>) {
  return shellRuntimeEventBus.on('unauthorized', () => {
    void handler();
  });
}

function createRuntimeDecision(
  menu: ShellMenu,
  moduleConfig: MangoModuleRuntimeConfig | undefined,
  resolvedPageType: string
): RuntimeDecision {
  return {
    menuName: menu.menuName,
    path: menu.path,
    component: menu.component,
    moduleCode: menu.moduleCode,
    menuPageType: menu.pageType,
    resolvedPageType,
    runtimeMode: moduleConfig?.mode,
    runtimeCode: moduleConfig?.runtimeCode,
    entry: moduleConfig?.entry,
    decidedAt: new Date().toISOString(),
  };
}

function applyRuntimeMarker(container: HTMLElement, decision: RuntimeDecision) {
  container.dataset.mangoRuntimePageType = decision.resolvedPageType;
  container.dataset.mangoRuntimeMode = decision.runtimeMode || 'local';
  container.dataset.mangoRuntimeModule = decision.moduleCode || '';
  container.dataset.mangoRuntimeCode = decision.runtimeCode || '';
  container.dataset.mangoRuntimeEntry = decision.entry || '';
}

function recordRuntimeDecision(decision: RuntimeDecision) {
  lastRuntimeDecision = decision;
  if (typeof window !== 'undefined') {
    (window as any).__MANGO_RUNTIME_DEBUG__ = decision;
  }
  if (!import.meta.env.DEV && !getMangoAdminShellOptions().runtimeDebug) {
    return;
  }
  console.debug('[mango-runtime] menu decision', decision);
}

function recordRuntimeConfigDiagnostics(diagnostics?: MangoRuntimeConfigDiagnostic[]) {
  lastRuntimeConfigDiagnostics = diagnostics || [];
  if (typeof window !== 'undefined') {
    (window as any).__MANGO_RUNTIME_CONFIG_DIAGNOSTICS__ = lastRuntimeConfigDiagnostics;
  }
  if ((!import.meta.env.DEV && !getMangoAdminShellOptions().runtimeDebug) || !diagnostics?.length) {
    return;
  }
  console.warn('[mango-runtime] config diagnostics', diagnostics);
}

function resolveShellApiBaseUrl() {
  const apiBaseUrl = getMangoAdminShellOptions().apiBaseUrl || '/api';
  if (/^https?:\/\//.test(apiBaseUrl)) {
    return apiBaseUrl;
  }
  if (typeof window === 'undefined') {
    return apiBaseUrl;
  }
  return new URL(apiBaseUrl, window.location.origin).toString();
}

export function getLastShellRuntimeDecision() {
  return lastRuntimeDecision;
}

export function getLastShellRuntimeConfigDiagnostics() {
  return [...lastRuntimeConfigDiagnostics];
}

if (typeof window !== 'undefined' && (import.meta.env.DEV || import.meta.env.VITE_MANGO_E2E === 'true')) {
  (window as any).__MANGO_RUNTIME_EVENT_BUS__ = shellRuntimeEventBus;
}
