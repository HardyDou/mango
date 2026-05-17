import type { Component } from 'vue';
import type { RouteRecordRaw } from 'vue-router';
import { destroyApp, startApp } from 'wujie';

export type MangoFrontendAppType = 'LOCAL' | 'MICRO_APP' | 'IFRAME' | 'EXTERNAL_LINK';
export type MangoDeployMode = 'EMBEDDED' | 'REMOTE' | 'HYBRID';
export type MangoStyleIsolation = 'NONE' | 'SCOPED' | 'SHADOW_DOM' | 'IFRAME';
export type MangoMenuPageType = 'LOCAL_ROUTE' | 'MICRO_ROUTE' | 'IFRAME' | 'EXTERNAL_LINK' | 'BUTTON';
export type MangoRuntimeProfile = 'monolith' | 'hybrid' | 'micro';
export type MangoModuleRuntimeMode = 'local' | 'micro';
export type MangoPageLoader = () => Promise<Component | { default?: Component } | unknown>;

export interface MangoMenu {
  appCode?: string;
  moduleCode?: string;
  pageType?: MangoMenuPageType;
  menuName: string;
  menuCode: string;
  path?: string;
  component?: string;
  redirect?: string;
  externalUrl?: string;
  meta?: Record<string, unknown>;
  children?: MangoMenu[];
}

export interface MangoAppRuntime {
  token: string;
  tenantId?: string | number;
  appCode: string;
  apiBaseUrl?: string;
  menu?: MangoMenu;
  userInfo: unknown;
  permissions: string[];
  theme: unknown;
  request: unknown;
  eventBus: unknown;
}

export interface MangoFrontendApp {
  appCode: string;
  name: string;
  routes?: RouteRecordRaw[];
  menus?: MangoMenu[];
  permissions?: string[];
  mount?: (container: HTMLElement, props: MangoAppRuntime) => void | Promise<void>;
  unmount?: () => void | Promise<void>;
}

export interface MangoRuntimeAppConfig {
  appCode: string;
  appName: string;
  appType: MangoFrontendAppType;
  deployMode: MangoDeployMode;
  entryUrl?: string;
  styleUrl?: string;
  mountPath?: string;
  activeRule?: string;
  framework?: string;
  version?: string;
  healthCheckUrl?: string;
  sandboxEnabled?: boolean;
  styleIsolation?: MangoStyleIsolation;
  icon?: string;
  sort?: number;
  status: number;
  timeoutMs?: number;
}

export interface MangoModuleRuntimeConfig {
  mode: MangoModuleRuntimeMode;
  entry?: string;
  style?: string;
  runtimeCode?: string;
  appType?: MangoFrontendAppType;
  framework?: string;
  timeoutMs?: number;
}

export interface MangoRuntimeConfig {
  profile: MangoRuntimeProfile;
  modules: Record<string, MangoModuleRuntimeConfig>;
}

export interface MangoPageRegistration {
  moduleCode: string;
  component: string;
  loader: MangoPageLoader;
}

export interface MangoPageRegistry {
  moduleCode: string;
  pages: Record<string, MangoPageLoader>;
}

export interface MangoAppAdapter {
  type: MangoFrontendAppType;
  mount: (config: MangoRuntimeAppConfig, container: HTMLElement, runtime: MangoAppRuntime) => Promise<void>;
  unmount?: (config: MangoRuntimeAppConfig) => Promise<void>;
}

export interface MangoMicroAppModule {
  mount: (container: HTMLElement, runtime: MangoAppRuntime) => void | Promise<void>;
  unmount?: () => void | Promise<void>;
}

export class MangoRuntimeError extends Error {
  appCode?: string;
  entryUrl?: string;

  constructor(message: string, config?: MangoRuntimeAppConfig, options?: { cause?: unknown }) {
    super(message);
    this.name = 'MangoRuntimeError';
    this.appCode = config?.appCode;
    this.entryUrl = config?.entryUrl;
    if (options?.cause) {
      (this as any).cause = options.cause;
    }
  }
}

const localApps = new Map<string, MangoFrontendApp>();
const wujieDestroyers = new Map<string, Function>();

type MangoMicroAppDebugEvent = {
  appCode: string;
  entryUrl?: string;
  phase: 'load' | 'mount' | 'unmount';
  at: string;
};

export function registerLocalApp(app: MangoFrontendApp) {
  localApps.set(app.appCode, app);
}

export function getLocalApp(appCode: string) {
  return localApps.get(appCode);
}

export function registerMicroApp(appCode: string, app: MangoMicroAppModule) {
  void appCode;
  void app;
}

export function getMicroApp(appCode: string) {
  void appCode;
  return undefined;
}

export async function loadRuntimeConfig(defaultConfig: MangoRuntimeConfig): Promise<MangoRuntimeConfig> {
  if (typeof fetch !== 'function') {
    return defaultConfig;
  }
  try {
    const response = await fetch('/runtime-config.json', {
      cache: 'no-store',
      headers: { Accept: 'application/json' },
    });
    if (!response.ok) {
      return defaultConfig;
    }
    const remote = await response.json();
    return normalizeRuntimeConfig(mergeRuntimeConfig(defaultConfig, remote));
  } catch (error) {
    return defaultConfig;
  }
}

export function mergeRuntimeConfig(base: MangoRuntimeConfig, override?: Partial<MangoRuntimeConfig>): MangoRuntimeConfig {
  return {
    profile: override?.profile || base.profile,
    modules: {
      ...base.modules,
      ...(override?.modules || {}),
    },
  };
}

export const localAdapter: MangoAppAdapter = {
  type: 'LOCAL',
  async mount(config, container, runtime) {
    const app = getLocalApp(config.appCode);
    if (!app?.mount) {
      return;
    }
    await app.mount(container, runtime);
  },
  async unmount(config) {
    await getLocalApp(config.appCode)?.unmount?.();
  },
};

export const microAppAdapter: MangoAppAdapter = {
  type: 'MICRO_APP',
  async mount(config, container, runtime) {
    if (!config.entryUrl) {
      throw new MangoRuntimeError(`Missing Mango micro app entry: ${config.appCode}`, config);
    }
    await microAppAdapter.unmount?.(config);
    container.innerHTML = '';
    recordMicroAppDebug(config, 'load');
    try {
      const destroy = await startApp({
        name: config.appCode,
        url: config.entryUrl,
        el: container,
        props: {
          mangoRuntime: runtime,
          mangoConfig: config,
        },
        alive: false,
        sync: false,
        fiber: true,
        attrs: {
          'data-mango-app': config.appCode,
        },
        degradeAttrs: {
          'data-mango-app': config.appCode,
        },
        loadError(url, error) {
          throw new MangoRuntimeError(`Failed to load Mango micro app: ${config.appCode} (${url})`, config, { cause: error });
        },
      });
      if (typeof destroy === 'function') {
        wujieDestroyers.set(config.appCode, destroy);
      }
    } catch (error) {
      throw error instanceof MangoRuntimeError
        ? error
        : new MangoRuntimeError(`Failed to mount Mango micro app: ${config.appCode}`, config, { cause: error });
    }
    recordMicroAppDebug(config, 'mount');
  },
  async unmount(config) {
    const destroy = wujieDestroyers.get(config.appCode);
    if (destroy) {
      destroy();
      wujieDestroyers.delete(config.appCode);
    }
    destroyApp(config.appCode);
    recordMicroAppDebug(config, 'unmount');
  },
};

export const iframeAdapter: MangoAppAdapter = {
  type: 'IFRAME',
  async mount(config, container) {
    if (!config.entryUrl) {
      return;
    }
    container.innerHTML = '';
    const iframe = document.createElement('iframe');
    iframe.src = config.entryUrl;
    iframe.style.width = '100%';
    iframe.style.height = '100%';
    iframe.style.border = '0';
    iframe.setAttribute('data-mango-app', config.appCode);
    container.appendChild(iframe);
  },
  async unmount(_config) {},
};

export const linkAdapter: MangoAppAdapter = {
  type: 'EXTERNAL_LINK',
  async mount(config) {
    if (config.entryUrl) {
      window.open(config.entryUrl, '_blank', 'noopener,noreferrer');
    }
  },
};

export function resolveAdapter(type: MangoFrontendAppType): MangoAppAdapter {
  if (type === 'MICRO_APP') return microAppAdapter;
  if (type === 'IFRAME') return iframeAdapter;
  if (type === 'EXTERNAL_LINK') return linkAdapter;
  return localAdapter;
}

function normalizeRuntimeConfig(config: MangoRuntimeConfig): MangoRuntimeConfig {
  const modules = Object.entries(config.modules || {}).reduce<Record<string, MangoModuleRuntimeConfig>>((acc, [moduleCode, module]) => {
    const mode = module?.mode === 'micro' ? 'micro' : 'local';
    acc[moduleCode] = {
      ...module,
      mode,
      appType: module?.appType || (mode === 'micro' ? 'MICRO_APP' : 'LOCAL'),
      timeoutMs: Number(module?.timeoutMs || 15000),
    };
    return acc;
  }, {});
  return {
    profile: config.profile || 'monolith',
    modules,
  };
}

function recordMicroAppDebug(config: MangoRuntimeAppConfig, phase: MangoMicroAppDebugEvent['phase']) {
  if (!import.meta.env.DEV || typeof window === 'undefined') {
    return;
  }
  const event: MangoMicroAppDebugEvent = {
    appCode: config.appCode,
    entryUrl: config.entryUrl,
    phase,
    at: new Date().toISOString(),
  };
  const runtimeWindow = window as any;
  const events = Array.isArray(runtimeWindow.__MANGO_MICRO_APP_EVENTS__)
    ? runtimeWindow.__MANGO_MICRO_APP_EVENTS__
    : [];
  events.push(event);
  runtimeWindow.__MANGO_MICRO_APP_EVENTS__ = events.slice(-50);
  runtimeWindow.__MANGO_ACTIVE_MICRO_APP__ = phase === 'unmount' ? undefined : {
    appCode: config.appCode,
    entryUrl: config.entryUrl,
    mountedAt: event.at,
  };
}
