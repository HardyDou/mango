import { computed, nextTick, ref, type Ref } from 'vue';
import type { Router } from 'vue-router';
import { del, get, post, put } from '@mango/common/utils/request';
import { Session } from '@mango/common/utils/storage';
import { createMangoHttpClient, type MangoHttpClient } from '@mango/http-client';
import {
  createRuntimeEventBus,
  emitMangoRuntimeLog,
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
import { getMangoAdminShellOptions } from '../config';
import { ensureDevCenterPagesRegistered, MenuTypeEnum, type ShellMenu, type ShellRouteMenu } from './menuHost';
import { ensureFeatureRegistrars } from './featureRegistrars';
import { defaultRuntimeConfig, loadShellRuntimeConfig } from './runtimeConfig';
import { resolveRuntimeAppConfig, toRuntimeApps } from './runtimeIdentity';
import { createLocalPageCacheHost, type LocalPageEntry } from './localPageCache';

export { resolveRuntimeAppConfig, toRuntimeApps } from './runtimeIdentity';

const shellRuntimeEventBus = createRuntimeEventBus();
const runtimeHttpClients = new Map<string, MangoHttpClient>();

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
  let localHostRoot: HTMLElement | undefined;
  let externalRoot: HTMLElement | undefined;
  let localPageHost: ReturnType<typeof createLocalPageCacheHost> | undefined;
  let currentMenu: ShellMenu | undefined;
  let mountSeq = 0;
  let defaultPagesPromise: Promise<void> | undefined;
  let activeTabKey = '';
  const detachedTabNodes = new Map<string, Node[]>();
  const disposedTabKeys = new Set<string>();
  const mountedTabs = new Map<
    string,
    {
      microConfig?: MangoRuntimeAppConfig;
      runtime?: MangoAppRuntime;
      externalNodes?: Node[];
      keepAlive: boolean;
    }
  >();

  function ensureRenderRoots(container: HTMLElement) {
    if (!localHostRoot) {
      localHostRoot = document.createElement('div');
      localHostRoot.className = 'mango-runtime-local-root';
    }
    if (!externalRoot) {
      externalRoot = document.createElement('div');
      externalRoot.className = 'mango-runtime-external-root';
    }
    if (localHostRoot.parentNode !== container) {
      container.appendChild(localHostRoot);
    }
    if (externalRoot.parentNode !== container) {
      container.appendChild(externalRoot);
    }
  }

  function ensureLocalPageHost() {
    if (localPageHost || !localHostRoot) {
      return;
    }
    localPageHost = createLocalPageCacheHost(localHostRoot, router);
  }

  function setLocalPage(entry: LocalPageEntry, keepAlive: boolean) {
    ensureLocalPageHost();
    localPageHost?.activate(entry, keepAlive);
  }

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
      if (error instanceof MangoRuntimeConfigError) {
        recordRuntimeConfigDiagnostics(error.diagnostics);
      }
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function mountMenu(menu?: ShellMenu | ShellRouteMenu, tabKey = 'default') {
    await nextTick();
    if (!menu) {
      return;
    }
    disposedTabKeys.delete(tabKey);
    const seq = ++mountSeq;
    const sourceMenu = normalizeMenu(menu);
    currentMenu = sourceMenu;

    const container = containerRef.value;
    if (!container) {
      return;
    }

    ensureRenderRoots(container);
    const moduleConfig = resolveModuleConfig(sourceMenu);
    const pageType = resolvePageType(sourceMenu, moduleConfig);
    const keepAlive = resolveMenuKeepAlive(sourceMenu);
    if (activeTabKey && activeTabKey !== tabKey) {
      await deactivateTab(activeTabKey);
    }
    activeTabKey = tabKey;
    restoreExternalTab(tabKey);
    if (pageType === 'LOCAL_ROUTE') {
      externalRoot?.replaceChildren();
    }
    const existingExternal = mountedTabs.get(tabKey);
    if (existingExternal?.keepAlive && existingExternal.externalNodes?.length && pageType !== 'LOCAL_ROUTE') {
      return;
    }
    if (!isMountAllowed(seq, tabKey)) {
      return;
    }
    runtimeDecision.value = createRuntimeDecision(sourceMenu, moduleConfig, pageType);
    recordRuntimeDecision(runtimeDecision.value);
    applyRuntimeMarker(externalRoot || container, runtimeDecision.value);
    if (!runtimeConfigAvailable.value && pageType === 'MICRO_ROUTE') {
      await mountFallback();
      return;
    }
    if (pageType === 'IFRAME') {
      if (!isMountAllowed(seq, tabKey)) {
        return;
      }
      mountIframe(sourceMenu, tabKey, keepAlive);
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
        await mountMicroMenu(sourceMenu, moduleConfig, seq, tabKey, keepAlive);
      } catch (error) {
        if (isMountAllowed(seq, tabKey)) {
          mountRuntimeError(sourceMenu, error, moduleConfig);
        }
      }
      return;
    }
    try {
      await mountLocalMenu(sourceMenu, seq, tabKey, keepAlive);
    } catch (error) {
      if (isMountAllowed(seq, tabKey)) {
        mountRuntimeError(sourceMenu, error, moduleConfig);
      }
    }
  }

  async function mountLocalMenu(menu: ShellMenu, seq: number, tabKey: string, keepAlive: boolean) {
    if (!containerRef.value) {
      return;
    }

    const cached = keepAlive ? localPageHost?.cachedPages.value.get(tabKey) : undefined;
    if (cached) {
      setLocalPage(cached, true);
      return;
    }

    await ensureDefaultPages();
    if (!isMountAllowed(seq, tabKey)) {
      return;
    }
    const loader = getPageLoader(menu.moduleCode, menu.component) || getPageLoader(undefined, menu.component);
    if (!loader) {
      if (menu.menuType === MenuTypeEnum.MENU && !menu.component) {
        mountMenuContractError(menu);
        return;
      }
      await mountNotFound(seq, tabKey, keepAlive);
      return;
    }

    const module = await loader();
    if (!isMountAllowed(seq, tabKey)) {
      return;
    }
    const component = module.default || module;
    const runtime = createLocalRuntime(menu, tabKey);
    setLocalPage({ tabKey, component, runtime }, keepAlive);
  }

  async function mountMicroMenu(
    menu: ShellMenu,
    moduleConfig: MangoModuleRuntimeConfig | undefined,
    seq: number,
    tabKey: string,
    keepAlive: boolean,
  ) {
    const config = resolveRuntimeConfig(menu, moduleConfig);
    const container = externalRoot;
    if (!config || !container) {
      mountRuntimeConfigError(menu, moduleConfig);
      return;
    }
    // Wujie identifies a mounted app by instanceId. Keep one instance per tab
    // so opening the same micro route for another record cannot unmount the
    // previously cached tab.
    const tabConfig = createTabRuntimeConfig(config, tabKey);
    activeRuntimeApp.value = tabConfig;
    const adapter = resolveAdapter(tabConfig.appType || 'MICRO_APP');
    const runtime = createRuntime(tabConfig, menu);
    try {
      await adapter.mount(tabConfig, container, runtime);
    } catch (error) {
      runtime.dispose?.();
      throw error;
    }
    if (!isMountAllowed(seq, tabKey)) {
      await adapter.unmount?.(tabConfig);
      runtime.dispose?.();
      return;
    }
    mountedTabs.set(tabKey, { microConfig: tabConfig, runtime, keepAlive });
  }

  async function mountNotFound(seq: number, tabKey: string, keepAlive: boolean) {
    await ensureDefaultPages();
    if (!isMountAllowed(seq, tabKey)) {
      return;
    }
    const loader = getPageLoader('mango-shell', 'error/404') || getPageLoader(undefined, 'error/404');
    if (!loader) {
      mountMessage('404');
      return;
    }
    const module = await loader();
    if (!isMountAllowed(seq, tabKey)) {
      return;
    }
    const component = (module as any).default || module;
    const runtime = createLocalRuntime(
      {
        appCode: 'internal-admin',
        moduleCode: 'mango-shell',
        menuId: 'shell-not-found',
        menuName: '404',
        menuCode: 'shell:not-found',
        parentId: 0,
        menuType: MenuTypeEnum.MENU,
        path: '/404',
        component: 'error/404',
        sort: -999,
        status: 1,
        visible: 0,
        keepAlive: keepAlive ? 1 : 0,
      },
      tabKey,
    );
    setLocalPage({ tabKey, component, runtime }, keepAlive);
  }

  async function retryCurrentMenu() {
    if (!currentMenu) {
      return;
    }
    await mountMenu(currentMenu, activeTabKey);
  }

  async function refreshTab(tabKey = activeTabKey) {
    if (!currentMenu || tabKey !== activeTabKey) {
      return;
    }
    disposedTabKeys.add(tabKey);
    detachedTabNodes.delete(tabKey);
    localPageHost?.deactivate(tabKey);
    localPageHost?.remove(tabKey);
    await unmountCurrentPage(tabKey);
    disposedTabKeys.delete(tabKey);
    await mountMenu(currentMenu, tabKey);
  }

  function mountIframe(menu: ShellMenu, tabKey: string, keepAlive: boolean) {
    const container = externalRoot;
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
    mountedTabs.set(tabKey, { externalNodes: [iframe], keepAlive });
  }

  function resolveRuntimeConfig(menu: ShellMenu, moduleConfig?: MangoModuleRuntimeConfig) {
    const runtimeCode = resolveRuntimeCode(menu, moduleConfig);
    if (!runtimeCode) {
      return undefined;
    }
    return resolveRuntimeAppConfig(runtimeApps.value, runtimeCode, moduleConfig?.instanceId);
  }

  function ensureDefaultPages() {
    if (!defaultPagesPromise) {
      defaultPagesPromise = import('@mango/admin-pages/defaults').then(async ({ registerDefaultAdminPages }) => {
        registerDefaultAdminPages({
          features: getMangoAdminShellOptions().features,
          shellPages: {
            home: () => import('../views/home/index.vue'),
            notFound: () => import('../views/error/404.vue'),
          },
          registries: [
            {
              moduleCode: 'mango-shell',
              pages: {
                'home/management/index': () => import('../views/home/management/index.vue'),
                'home/templates/index': () => import('../views/home/templates/index.vue'),
                'home/list/index': () => import('../views/home/list/index.vue'),
                'home/user/index': () => import('../views/home/user/index.vue'),
              },
            },
          ],
        });
        await ensureFeatureRegistrars();
        return ensureDevCenterPagesRegistered();
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

  async function unmountCurrentPage(tabKey = activeTabKey) {
    const mounted = mountedTabs.get(tabKey);
    if (!mounted) {
      return;
    }
    if (mounted.microConfig) {
      await resolveAdapter(mounted.microConfig.appType || 'MICRO_APP').unmount?.(mounted.microConfig);
    }
    mounted.runtime?.dispose?.();
    mounted.externalNodes?.forEach((node) => node.parentNode?.removeChild(node));
    mountedTabs.delete(tabKey);
  }

  async function deactivateTab(tabKey: string) {
    localPageHost?.deactivate(tabKey);
    const mounted = mountedTabs.get(tabKey);
    if (!mounted) {
      return;
    }
    if (mounted.keepAlive) {
      detachExternalTab(tabKey);
      return;
    }
    await unmountCurrentPage(tabKey);
  }

  function detachExternalTab(tabKey: string) {
    const container = externalRoot;
    const mounted = mountedTabs.get(tabKey);
    if (!container || !mounted || detachedTabNodes.has(tabKey)) {
      return;
    }
    const nodes = mounted.externalNodes || Array.from(container.childNodes);
    mounted.externalNodes = nodes;
    detachedTabNodes.set(tabKey, nodes);
    container.replaceChildren();
  }

  function restoreExternalTab(tabKey: string) {
    const nodes = detachedTabNodes.get(tabKey);
    if (!nodes || !externalRoot) {
      return;
    }
    externalRoot.replaceChildren(...nodes);
    detachedTabNodes.delete(tabKey);
  }

  function mountMessage(message: string) {
    const container = externalRoot || containerRef.value;
    if (!container) {
      return;
    }
    renderRuntimeState(container, {
      title: message,
      description: '请检查菜单运行类型、组件路径或运行配置。',
    });
  }

  function mountRuntimeError(menu: ShellMenu, error: unknown, moduleConfig?: MangoModuleRuntimeConfig) {
    const container = externalRoot || containerRef.value;
    if (!container) {
      return;
    }
    const errorMessage = error instanceof Error ? error.message : '页面加载失败';
    const entry = moduleConfig?.entry || activeRuntimeApp.value?.entryUrl || '';
    const runtimeCode = moduleConfig?.runtimeCode || activeRuntimeApp.value?.appCode || menu.moduleCode || '';
    renderRuntimeState(container, {
      title: '页面加载失败',
      description: `${menu.menuName || menu.path || '当前页面'}：${errorMessage}`,
      details: [runtimeCode ? `运行单元：${runtimeCode}` : '', entry ? `入口地址：${entry}` : ''].filter(Boolean),
      retry: retryCurrentMenu,
    });
  }

  function mountRuntimeConfigError(menu: ShellMenu, moduleConfig?: MangoModuleRuntimeConfig) {
    const container = externalRoot || containerRef.value;
    if (!container) {
      return;
    }
    const runtimeCode = resolveRuntimeCode(menu, moduleConfig);
    const diagnostics = findRuntimeDiagnostics(menu, moduleConfig);
    renderRuntimeState(container, {
      title: `缺少微应用运行配置：${runtimeCode}`,
      details: diagnostics.length
        ? diagnostics.map((item) => item.message)
        : ['请检查 runtime-config.json 是否配置 entry 和 runtimeCode。'],
      retry: retryCurrentMenu,
    });
  }

  function mountMenuContractError(menu: ShellMenu) {
    const container = externalRoot || containerRef.value;
    if (!container) {
      return;
    }
    const diagnostics = menu.meta?.diagnostics || [];
    renderRuntimeState(container, {
      title: '菜单配置错误',
      description:
        diagnostics[0] || `${menu.menuName || menu.path || '当前菜单'} 缺少 component，无法作为本地页面挂载。`,
      details: [
        `菜单编码：${menu.menuCode || '-'}`,
        `菜单路径：${menu.path || '-'}`,
        '请将分组菜单配置为目录，或为页面菜单补充 component。',
      ],
    });
  }

  function findRuntimeDiagnostics(menu: ShellMenu, moduleConfig?: MangoModuleRuntimeConfig) {
    const moduleCode = menu.moduleCode;
    const runtimeCode = moduleConfig?.runtimeCode;
    return (runtimeConfig.value.diagnostics || []).filter(
      (item) => item.moduleCode === moduleCode || item.moduleCode === runtimeCode,
    );
  }

  async function mountFallback() {
    await nextTick();
    const container = externalRoot || containerRef.value;
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
    disposedTabKeys.clear();
    detachedTabNodes.clear();
    localPageHost?.dispose();
    localPageHost = undefined;
    return Promise.all(Array.from(mountedTabs.keys()).map((tabKey) => unmountCurrentPage(tabKey)));
  }

  async function disposeTab(tabKey: string) {
    disposedTabKeys.add(tabKey);
    detachedTabNodes.delete(tabKey);
    localPageHost?.deactivate(tabKey);
    localPageHost?.remove(tabKey);
    await unmountCurrentPage(tabKey);
  }

  function isLatestMount(seq: number) {
    return seq === mountSeq;
  }

  function isMountAllowed(seq: number, tabKey: string) {
    return isLatestMount(seq) && !disposedTabKeys.has(tabKey);
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
    refreshTab,
    disposeTab,
    dispose,
  };
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
    .filter((app) => app.preload)
    .forEach((app) => {
      try {
        preloadMicroApp(app, createBaseRuntime(app));
      } catch (error) {
        console.warn('[mango-runtime] preload failed', app.appCode, error);
      }
    });
}

function normalizeMenu(menu: ShellMenu | ShellRouteMenu): ShellMenu {
  return 'sourceMenu' in menu ? menu.sourceMenu : menu;
}

function resolveMenuKeepAlive(menu: ShellMenu | ShellRouteMenu): boolean {
  const sourceMenu = normalizeMenu(menu);
  return sourceMenu.keepAlive === 1 || (menu as ShellRouteMenu).meta?.keepAlive === true;
}

function renderRuntimeState(
  container: HTMLElement,
  options: {
    title: string;
    description?: string;
    details?: string[];
    retry?: () => void | Promise<void>;
  },
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
    retry.addEventListener(
      'click',
      () => {
        void options.retry?.();
      },
      { once: true },
    );
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

function createLocalRuntime(menu: ShellMenu, tabKey: string) {
  const appCode = menu.moduleCode || 'mango-admin-local';
  return createRuntime(
    {
      appCode,
      instanceId: `local:${appCode}::tab-${hashTabKey(tabKey)}`,
      appName: appCode,
      appType: 'LOCAL',
      deployMode: 'LOCAL',
      status: 1,
    },
    menu,
  );
}

function createBaseRuntime(config: MangoRuntimeAppConfig): MangoAppRuntime {
  const userInfo = Session.get('userInfo') || {};
  const instanceId = config.instanceId?.trim() || config.appCode;
  let httpClient = runtimeHttpClients.get(instanceId);
  if (!httpClient || httpClient.state === 'destroyed') {
    httpClient = createMangoHttpClient({
      baseUrl: window.location.origin + '/api',
      getAccessToken: () => Session.getToken?.() || '',
      getTenantId: () => (Session.get('userInfo') || {}).tenantId,
      onUnauthorized: () => shellRuntimeEventBus.emit('unauthorized'),
    });
    runtimeHttpClients.set(instanceId, httpClient);
  }
  return {
    instanceId,
    token: Session.getToken?.() || '',
    tenantId: userInfo.tenantId,
    appCode: config.appCode,
    apiBaseUrl: window.location.origin + '/api',
    menu: undefined,
    userInfo,
    permissions: userInfo.permissions || [],
    request: {
      get,
      post,
      put,
      delete: del,
    },
    httpClient,
    dispose: () => {
      if (runtimeHttpClients.get(instanceId) !== httpClient) return;
      runtimeHttpClients.delete(instanceId);
      httpClient.destroy();
    },
    eventBus: shellRuntimeEventBus,
    theme: createShellRuntimeTheme(),
  };
}

function createTabRuntimeConfig(config: MangoRuntimeAppConfig, tabKey: string): MangoRuntimeAppConfig {
  const baseInstanceId = config.instanceId?.trim() || config.appCode;
  if (!tabKey || tabKey === 'default') {
    return { ...config, instanceId: baseInstanceId };
  }
  return {
    ...config,
    instanceId: `${baseInstanceId}::tab-${hashTabKey(tabKey)}`,
  };
}

function hashTabKey(value: string): string {
  let hash = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0).toString(16).padStart(8, '0');
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
      '--mango-color-top-bar': themeStore.topBarColor,
      '--mango-bg-menu-bar': themeStore.menuBar,
      '--mango-color-menu-bar': themeStore.menuBarColor,
      '--mango-color-menu-active-bg': themeStore.menuBarActiveColor,
      '--mango-bg-columns-menu-bar': themeStore.columnsMenuBar,
      '--mango-color-columns-menu-bar': themeStore.columnsMenuBarColor,
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
  resolvedPageType: string,
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
  if (typeof window !== 'undefined') {
    (window as any).__MANGO_RUNTIME_DEBUG__ = decision;
  }
  if (!import.meta.env.DEV) {
    return;
  }
  console.debug('[mango-runtime] menu decision', decision);
}

function recordRuntimeConfigDiagnostics(diagnostics?: MangoRuntimeConfigDiagnostic[]) {
  if (typeof window !== 'undefined') {
    (window as any).__MANGO_RUNTIME_CONFIG_DIAGNOSTICS__ = diagnostics || [];
  }
  if (!import.meta.env.DEV || !diagnostics?.length) {
    return;
  }
  console.warn('[mango-runtime] config diagnostics', diagnostics);
}

if (typeof window !== 'undefined' && (import.meta.env.DEV || import.meta.env.VITE_MANGO_E2E === 'true')) {
  (window as any).__MANGO_RUNTIME_EVENT_BUS__ = shellRuntimeEventBus;
}
