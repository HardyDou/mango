import {
  loadRuntimeConfigWithOptions,
  type MangoRuntimeConfig,
  type MangoRuntimeConfigLoadOptions,
} from '@mango/app-runtime';
import { getMangoAdminShellOptions } from '../config';

const importMetaEnv = import.meta.env;
const runtimeEnvKey = '__MANGO_IMPORT_META_ENV__';
const globalRuntime = globalThis as typeof globalThis & {
  [runtimeEnvKey]?: Record<string, unknown>;
};

if (!globalRuntime[runtimeEnvKey]) {
  globalRuntime[runtimeEnvKey] = importMetaEnv as Record<string, unknown>;
}

const isE2E = readRuntimeEnvBoolean('VITE_MANGO_E2E');

export const defaultRuntimeConfig: MangoRuntimeConfig = {
  profile: (readRuntimeEnvString('VITE_MANGO_RUNTIME_PROFILE') || 'monolith') as MangoRuntimeConfig['profile'],
  modules: {
    'mango-authorization': {
      mode: (readRuntimeEnvString('VITE_MANGO_RBAC_MODE') || 'local') as any,
      entry: readRuntimeEnvString('VITE_MANGO_RBAC_ENTRY') || 'http://127.0.0.1:5181/',
      runtimeCode: 'mango-admin-rbac-app',
      appType: 'MICRO_APP',
      framework: 'vue3',
    },
    'mango-system': {
      mode: (readRuntimeEnvString('VITE_MANGO_SYSTEM_MODE') || 'local') as any,
      runtimeCode: 'mango-admin-system-local',
      appType: 'LOCAL',
      framework: 'vue3',
    },
    'mango-workflow': {
      mode: (readRuntimeEnvString('VITE_MANGO_WORKFLOW_MODE') || 'local') as any,
      entry: readRuntimeEnvString('VITE_MANGO_WORKFLOW_ENTRY') || 'http://127.0.0.1:5182/',
      runtimeCode: 'mango-admin-workflow-app',
      appType: 'MICRO_APP',
      framework: 'vue3',
    },
  },
};

function resolveDefaultRuntimeConfig(): MangoRuntimeConfig {
  const options = getMangoAdminShellOptions();
  return {
    ...defaultRuntimeConfig,
    modules: {
      ...defaultRuntimeConfig.modules,
      ...options.modules,
    },
  };
}

function splitEnvList(value?: string) {
  return (value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

export function createShellRuntimeConfigOptions(): MangoRuntimeConfigLoadOptions {
  const options = getMangoAdminShellOptions();
  const allowedOrigins = splitEnvList(readRuntimeEnvString('VITE_MANGO_ALLOWED_REMOTE_ORIGINS'));
  const allowedHosts = splitEnvList(readRuntimeEnvString('VITE_MANGO_ALLOWED_REMOTE_HOSTS'));
  const currentDeployEnv = (readRuntimeEnvString('VITE_MANGO_DEPLOY_ENV') || readRuntimeEnvString('MODE')).toLowerCase();
  const explicitCurrentDeployEnv = readRuntimeEnvString('VITE_MANGO_DEPLOY_ENV').toLowerCase();
  const currentProduction = readRuntimeEnvBoolean('PROD');
  const isPrdLike =
    currentDeployEnv === 'prod'
    || currentDeployEnv === 'prd'
    || currentDeployEnv === 'production'
    || (currentProduction && !explicitCurrentDeployEnv);
  const allowHttpEntries = readRuntimeEnvBoolean('VITE_MANGO_ALLOW_HTTP_REMOTE_ENTRIES') && (!isPrdLike || isE2E);

  if (!currentProduction) {
    return {
      failClosed: false,
      allowHttpEntries: true,
      allowRelativeEntries: true,
      configUrl: options.runtimeConfigUrl,
      allowedEntryOrigins: allowedOrigins.length
        ? allowedOrigins
        : [
            'http://localhost:5181',
            'http://127.0.0.1:5181',
            'http://b.mango.io:5181',
            'http://localhost:5182',
            'http://127.0.0.1:5182',
            'http://c.mango.io:5182',
      ],
      allowedEntryHosts: allowedHosts,
      ...options.runtimeConfigLoadOptions,
    };
  }

  if (isPrdLike) {
    return {
      failClosed: true,
      allowHttpEntries,
      allowRelativeEntries: true,
      requireEntryAllowlist: true,
      configUrl: options.runtimeConfigUrl,
      allowedEntryOrigins: allowedOrigins,
      allowedEntryHosts: [],
      ...options.runtimeConfigLoadOptions,
    };
  }

  return {
    failClosed: true,
    allowHttpEntries,
    allowRelativeEntries: true,
    requireEntryAllowlist: true,
    configUrl: options.runtimeConfigUrl,
    allowedEntryOrigins: allowedOrigins,
    allowedEntryHosts: allowedHosts,
    ...options.runtimeConfigLoadOptions,
  };
}

export function loadShellRuntimeConfig() {
  return loadRuntimeConfigWithOptions(resolveDefaultRuntimeConfig(), createShellRuntimeConfigOptions());
}

function getRuntimeEnv() {
  return globalRuntime[runtimeEnvKey] || {};
}

function readRuntimeEnvString(name: string) {
  const value = getRuntimeEnv()[name];
  return typeof value === 'string' ? value : '';
}

function readRuntimeEnvBoolean(name: string) {
  const value = getRuntimeEnv()[name];
  return value === true || value === 'true';
}
