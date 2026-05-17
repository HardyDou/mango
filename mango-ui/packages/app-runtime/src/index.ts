import type { Component } from 'vue';
import type { RouteRecordRaw } from 'vue-router';
import { destroyApp, preloadApp, startApp } from 'wujie';

export type MangoFrontendAppType = 'LOCAL' | 'MICRO_APP' | 'IFRAME' | 'EXTERNAL_LINK';
export type MangoDeployMode = 'EMBEDDED' | 'REMOTE' | 'HYBRID';
export type MangoStyleIsolation = 'NONE' | 'SCOPED' | 'SHADOW_DOM' | 'IFRAME';
export type MangoMenuPageType = 'LOCAL_ROUTE' | 'MICRO_ROUTE' | 'IFRAME' | 'EXTERNAL_LINK' | 'BUTTON';
export type MangoRuntimeProfile = 'monolith' | 'hybrid' | 'micro';
export type MangoModuleRuntimeMode = 'local' | 'micro';
export type MangoPageLoader = () => Promise<Component | { default?: Component } | unknown>;
export type MangoRuntimeConfigDiagnosticLevel = 'warning' | 'error';

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
  preload?: boolean;
  alive?: boolean;
}

export interface MangoModuleRuntimeConfig {
  mode: MangoModuleRuntimeMode;
  entry?: string;
  style?: string;
  runtimeCode?: string;
  appType?: MangoFrontendAppType;
  framework?: string;
  timeoutMs?: number;
  preload?: boolean;
  alive?: boolean;
}

export interface MangoRuntimeConfig {
  profile: MangoRuntimeProfile;
  modules: Record<string, MangoModuleRuntimeConfig>;
  diagnostics?: MangoRuntimeConfigDiagnostic[];
}

export interface MangoRuntimeConfigDiagnostic {
  level: MangoRuntimeConfigDiagnosticLevel;
  moduleCode?: string;
  field?: string;
  message: string;
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

export class MangoRuntimeConfigError extends Error {
  diagnostics?: MangoRuntimeConfigDiagnostic[];

  constructor(message: string, diagnostics?: MangoRuntimeConfigDiagnostic[], options?: { cause?: unknown }) {
    super(message);
    this.name = 'MangoRuntimeConfigError';
    this.diagnostics = diagnostics;
    if (options?.cause) {
      (this as any).cause = options.cause;
    }
  }
}

const localApps = new Map<string, MangoFrontendApp>();
const wujieDestroyers = new Map<string, Function>();
const validProfiles = new Set<MangoRuntimeProfile>(['monolith', 'hybrid', 'micro']);
const validModes = new Set<MangoModuleRuntimeMode>(['local', 'micro']);

type MangoMicroAppDebugEvent = {
  appCode: string;
  entryUrl?: string;
  phase:
    | 'load'
    | 'preload'
    | 'before-load'
    | 'before-mount'
    | 'mount'
    | 'before-unmount'
    | 'unmount'
    | 'after-unmount'
    | 'load-error'
    | 'timeout';
  at: string;
  detail?: string;
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
    return normalizeRuntimeConfig(defaultConfig);
  }
  try {
    const response = await fetch('/runtime-config.json', {
      cache: 'no-store',
      headers: { Accept: 'application/json' },
    });
    if (!response.ok) {
      return normalizeRuntimeConfig(defaultConfig);
    }
    let remote: Partial<MangoRuntimeConfig>;
    try {
      remote = await response.json();
    } catch (error) {
      throw new MangoRuntimeConfigError('Invalid runtime-config.json: JSON parse failed', undefined, { cause: error });
    }
    return normalizeRuntimeConfig(mergeRuntimeConfig(defaultConfig, remote));
  } catch (error) {
    if (error instanceof MangoRuntimeConfigError) {
      throw error;
    }
    return normalizeRuntimeConfig(defaultConfig);
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
      const destroy = await withTimeout(startApp({
        name: config.appCode,
        url: config.entryUrl,
        el: container,
        props: {
          mangoRuntime: runtime,
          mangoConfig: config,
        },
        alive: config.alive === true,
        sync: false,
        fiber: true,
        attrs: {
          'data-mango-app': config.appCode,
        },
        degradeAttrs: {
          'data-mango-app': config.appCode,
        },
        beforeLoad: () => recordMicroAppDebug(config, 'before-load'),
        beforeMount: () => recordMicroAppDebug(config, 'before-mount'),
        afterMount: () => recordMicroAppDebug(config, 'mount'),
        beforeUnmount: () => recordMicroAppDebug(config, 'before-unmount'),
        afterUnmount: () => recordMicroAppDebug(config, 'after-unmount'),
        loadError(url, error) {
          recordMicroAppDebug(config, 'load-error', url);
          throw new MangoRuntimeError(`Failed to load Mango micro app: ${config.appCode} (${url})`, config, { cause: error });
        },
      }), config.timeoutMs || 15000, config);
      if (typeof destroy === 'function') {
        wujieDestroyers.set(config.appCode, destroy);
      }
    } catch (error) {
      destroyApp(config.appCode);
      throw error instanceof MangoRuntimeError
        ? error
        : new MangoRuntimeError(`Failed to mount Mango micro app: ${config.appCode}`, config, { cause: error });
    }
    recordMicroAppDebug(config, 'mount');
  },
  async unmount(config) {
    recordMicroAppDebug(config, 'before-unmount');
    const destroy = wujieDestroyers.get(config.appCode);
    if (destroy) {
      destroy();
      wujieDestroyers.delete(config.appCode);
    }
    destroyApp(config.appCode);
    recordMicroAppDebug(config, 'unmount');
  },
};

export function preloadMicroApp(config: MangoRuntimeAppConfig, runtime?: Partial<MangoAppRuntime>) {
  if (config.appType !== 'MICRO_APP' || !config.entryUrl) {
    return;
  }
  recordMicroAppDebug(config, 'preload');
  preloadApp({
    name: config.appCode,
    url: config.entryUrl,
    props: {
      mangoRuntime: runtime,
      mangoConfig: config,
    },
    alive: config.alive === true,
    fiber: true,
    attrs: {
      'data-mango-app': config.appCode,
    },
    degradeAttrs: {
      'data-mango-app': config.appCode,
    },
    loadError(url, error) {
      recordMicroAppDebug(config, 'load-error', url);
      throw new MangoRuntimeError(`Failed to preload Mango micro app: ${config.appCode} (${url})`, config, { cause: error });
    },
  });
}

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
  const diagnostics: MangoRuntimeConfigDiagnostic[] = [];
  const modules = Object.entries(config.modules || {}).reduce<Record<string, MangoModuleRuntimeConfig>>((acc, [moduleCode, module]) => {
    const mode = normalizeRuntimeMode(moduleCode, module?.mode, diagnostics);
    const timeoutMs = normalizeTimeout(moduleCode, module?.timeoutMs, diagnostics);
    acc[moduleCode] = {
      ...module,
      mode,
      appType: module?.appType || (mode === 'micro' ? 'MICRO_APP' : 'LOCAL'),
      timeoutMs,
    };
    if (mode === 'micro') {
      validateMicroModule(moduleCode, acc[moduleCode], diagnostics);
    }
    return acc;
  }, {});
  return {
    profile: normalizeRuntimeProfile(config.profile, diagnostics),
    modules,
    diagnostics,
  };
}

function normalizeRuntimeProfile(profile: unknown, diagnostics: MangoRuntimeConfigDiagnostic[]): MangoRuntimeProfile {
  if (typeof profile === 'string' && validProfiles.has(profile as MangoRuntimeProfile)) {
    return profile as MangoRuntimeProfile;
  }
  if (profile !== undefined) {
    diagnostics.push({
      level: 'error',
      field: 'profile',
      message: `Invalid runtime profile '${String(profile)}', fallback to monolith`,
    });
  }
  return 'monolith';
}

function normalizeRuntimeMode(
  moduleCode: string,
  mode: unknown,
  diagnostics: MangoRuntimeConfigDiagnostic[]
): MangoModuleRuntimeMode {
  if (typeof mode === 'string' && validModes.has(mode as MangoModuleRuntimeMode)) {
    return mode as MangoModuleRuntimeMode;
  }
  diagnostics.push({
    level: 'error',
    moduleCode,
    field: 'mode',
    message: `Invalid runtime mode '${String(mode)}', fallback to local`,
  });
  return 'local';
}

function normalizeTimeout(
  moduleCode: string,
  timeoutMs: unknown,
  diagnostics: MangoRuntimeConfigDiagnostic[]
) {
  const timeout = Number(timeoutMs || 15000);
  if (Number.isFinite(timeout) && timeout >= 1000) {
    return timeout;
  }
  diagnostics.push({
    level: 'warning',
    moduleCode,
    field: 'timeoutMs',
    message: `Invalid timeoutMs '${String(timeoutMs)}', fallback to 15000`,
  });
  return 15000;
}

function validateMicroModule(
  moduleCode: string,
  module: MangoModuleRuntimeConfig,
  diagnostics: MangoRuntimeConfigDiagnostic[]
) {
  if (!module.runtimeCode) {
    diagnostics.push({
      level: 'warning',
      moduleCode,
      field: 'runtimeCode',
      message: `Micro module '${moduleCode}' does not define runtimeCode, fallback to moduleCode`,
    });
  }
  if (!module.entry) {
    diagnostics.push({
      level: 'error',
      moduleCode,
      field: 'entry',
      message: `Micro module '${moduleCode}' is missing entry`,
    });
    return;
  }
  if (!isValidRuntimeEntry(module.entry)) {
    diagnostics.push({
      level: 'error',
      moduleCode,
      field: 'entry',
      message: `Micro module '${moduleCode}' has invalid entry '${module.entry}'`,
    });
  }
}

function isValidRuntimeEntry(entry: string) {
  if (entry.startsWith('/')) {
    return true;
  }
  try {
    const url = new URL(entry);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch (error) {
    return false;
  }
}

function withTimeout<T>(promise: Promise<T>, timeoutMs: number, config: MangoRuntimeAppConfig): Promise<T> {
  let timeoutId: ReturnType<typeof setTimeout> | undefined;
  const timeout = new Promise<never>((_, reject) => {
    timeoutId = setTimeout(() => {
      recordMicroAppDebug(config, 'timeout', `${timeoutMs}ms`);
      reject(new MangoRuntimeError(`Mango micro app load timeout: ${config.appCode} (${timeoutMs}ms)`, config));
    }, timeoutMs);
  });
  return Promise.race([promise, timeout]).finally(() => {
    if (timeoutId) {
      clearTimeout(timeoutId);
    }
  });
}

function recordMicroAppDebug(config: MangoRuntimeAppConfig, phase: MangoMicroAppDebugEvent['phase'], detail?: string) {
  if (!import.meta.env.DEV || typeof window === 'undefined') {
    return;
  }
  const event: MangoMicroAppDebugEvent = {
    appCode: config.appCode,
    entryUrl: config.entryUrl,
    phase,
    at: new Date().toISOString(),
    detail,
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
