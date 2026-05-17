import { computed, createApp, h, nextTick, ref, type App as VueApp, type Ref } from 'vue';
import type { Router } from 'vue-router';
import ElementPlus from 'element-plus';
import { get, Session } from '@mango/common';
import {
  loadRuntimeConfig,
  resolveAdapter,
  type MangoAppRuntime,
  type MangoModuleRuntimeConfig,
  type MangoRuntimeConfig,
  type MangoRuntimeAppConfig,
} from '@mango/app-runtime';
import { getPageLoader } from '@mango/admin-pages';
import type { ShellMenu, ShellRouteMenu } from './menuHost';
import { defaultRuntimeConfig, loadShellRuntimeConfig } from './runtimeConfig';

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
  let mountedLocalPage: VueApp | undefined;
  let mountedMicroConfig: MangoRuntimeAppConfig | undefined;
  let currentMenu: ShellMenu | undefined;

  async function loadRuntimeApps() {
    loading.value = true;
    try {
      runtimeConfig.value = await loadShellRuntimeConfig();
      runtimeApps.value = toRuntimeApps(runtimeConfig.value);
      return true;
    } catch (error) {
      runtimeApps.value = [];
      runtimeConfig.value = defaultRuntimeConfig;
      activeRuntimeApp.value = undefined;
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
    const sourceMenu = normalizeMenu(menu);
    currentMenu = sourceMenu;

    const container = containerRef.value;
    if (!container) {
      return;
    }

    await unmountCurrentPage();
    container.innerHTML = '';

    const moduleConfig = resolveModuleConfig(sourceMenu);
    const pageType = resolvePageType(sourceMenu, moduleConfig);
    runtimeDecision.value = createRuntimeDecision(sourceMenu, moduleConfig, pageType);
    recordRuntimeDecision(runtimeDecision.value);
    applyRuntimeMarker(container, runtimeDecision.value);
    if (pageType === 'IFRAME') {
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
        await mountMicroMenu(sourceMenu, moduleConfig);
      } catch (error) {
        mountRuntimeError(sourceMenu, error);
      }
      return;
    }
    try {
      await mountLocalMenu(sourceMenu);
    } catch (error) {
      mountRuntimeError(sourceMenu, error);
    }
  }

  async function mountLocalMenu(menu: ShellMenu) {
    const container = containerRef.value;
    if (!container) {
      return;
    }

    const loader = getPageLoader(menu.moduleCode, menu.component) || getPageLoader(undefined, menu.component);
    if (!loader) {
      mountMessage(`缺少本地组件映射：${menu.component || menu.path}`);
      return;
    }

    const module = await loader();
    const component = module.default || module;
    mountedLocalPage = createApp({ render: () => h(component) });
    mountedLocalPage.use(ElementPlus);
    mountedLocalPage.use(router);
    mountedLocalPage.mount(container);
  }

  async function mountMicroMenu(menu: ShellMenu, moduleConfig?: MangoModuleRuntimeConfig) {
    const config = resolveRuntimeConfig(menu, moduleConfig);
    const container = containerRef.value;
    if (!config || !container) {
      mountMessage(`缺少微应用运行配置：${moduleConfig?.runtimeCode || menu.moduleCode || menu.path}`);
      return;
    }
    activeRuntimeApp.value = config;
    mountedMicroConfig = config;
    const adapter = resolveAdapter(config.appType || 'MICRO_APP');
    await adapter.mount(config, container, createRuntime(config, menu));
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
    return runtimeApps.value.find(app => moduleConfig?.runtimeCode && app.appCode === moduleConfig.runtimeCode)
      || runtimeApps.value.find(app => menu.appCode && menu.appCode !== 'internal-admin' && app.appCode === menu.appCode)
      || runtimeApps.value.find(app => app.appCode === 'mango-admin-local')
      || runtimeApps.value.find(app => app.appCode === 'internal-admin')
      || runtimeApps.value[0];
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
    container.innerHTML = `
      <div class="micro-runtime-empty">
        <div>
          <h3>${message}</h3>
          <p>请检查菜单运行类型、组件路径或运行配置。</p>
        </div>
      </div>
    `;
  }

  function mountRuntimeError(menu: ShellMenu, error: unknown) {
    const container = containerRef.value;
    if (!container) {
      return;
    }
    const errorMessage = error instanceof Error ? error.message : '页面加载失败';
    container.innerHTML = `
      <div class="micro-runtime-empty">
        <div>
          <h3>页面加载失败</h3>
          <p>${escapeHtml(menu.menuName || menu.path || '当前页面')}：${escapeHtml(errorMessage)}</p>
          <button class="micro-runtime-retry" type="button">重试</button>
        </div>
      </div>
    `;
    container.querySelector('.micro-runtime-retry')?.addEventListener('click', () => {
      void retryCurrentMenu();
    }, { once: true });
  }

  async function mountFallback() {
    await nextTick();
    const container = containerRef.value;
    if (!container) {
      return;
    }
    container.innerHTML = `
      <div class="micro-runtime-empty">
        <div>
          <h3>运行配置加载失败</h3>
          <p>请确认登录态、租户开通关系和后端服务状态。</p>
        </div>
      </div>
    `;
  }

  return {
    loading,
    runtimeApps,
    runtimeConfig,
    activeRuntimeApp: computed(() => activeRuntimeApp.value),
    runtimeDecision: computed(() => runtimeDecision.value),
    loadRuntimeApps,
    mountMenu,
  };
}

function toRuntimeApps(config: MangoRuntimeConfig): MangoRuntimeAppConfig[] {
  return Object.entries(config.modules)
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
    }));
}

function normalizeMenu(menu: ShellMenu | ShellRouteMenu): ShellMenu {
  return 'sourceMenu' in menu ? menu.sourceMenu : menu;
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function createRuntime(config: MangoRuntimeAppConfig, menu?: ShellMenu): MangoAppRuntime {
  const userInfo = Session.get('userInfo') || {};
  return {
    token: Session.getToken?.() || '',
    tenantId: userInfo.tenantId,
    appCode: config.appCode,
    apiBaseUrl: window.location.origin + '/api',
    menu,
    userInfo,
    permissions: userInfo.permissions || [],
    request: get,
    eventBus: undefined,
    theme: {},
  };
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
  if (typeof window !== 'undefined') {
    (window as any).__MANGO_RUNTIME_DEBUG__ = decision;
  }
  if (!import.meta.env.DEV) {
    return;
  }
  console.debug('[mango-runtime] menu decision', decision);
}
