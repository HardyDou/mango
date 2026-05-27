import {
  loadRuntimeConfigWithOptions,
  type MangoRuntimeConfig,
  type MangoRuntimeConfigLoadOptions,
} from '@mango/app-runtime';
import { getMangoAdminShellOptions } from '../config';

const isProduction = import.meta.env.PROD;
const deployEnv = (import.meta.env.VITE_MANGO_DEPLOY_ENV || import.meta.env.MODE || '').toLowerCase();
const explicitDeployEnv = (import.meta.env.VITE_MANGO_DEPLOY_ENV || '').toLowerCase();
const isPrdLike =
  deployEnv === 'prod'
  || deployEnv === 'prd'
  || deployEnv === 'production'
  || (isProduction && !explicitDeployEnv);
const isE2E = import.meta.env.VITE_MANGO_E2E === 'true';

export const defaultRuntimeConfig: MangoRuntimeConfig = {
  profile: (import.meta.env.VITE_MANGO_RUNTIME_PROFILE || 'monolith') as MangoRuntimeConfig['profile'],
  modules: {
    'mango-authorization': {
      mode: (import.meta.env.VITE_MANGO_RBAC_MODE || 'local') as any,
      entry: import.meta.env.VITE_MANGO_RBAC_ENTRY || 'http://127.0.0.1:5181/',
      runtimeCode: 'mango-admin-rbac-app',
      appType: 'MICRO_APP',
      framework: 'vue3',
    },
    'mango-system': {
      mode: (import.meta.env.VITE_MANGO_SYSTEM_MODE || 'local') as any,
      runtimeCode: 'mango-admin-system-local',
      appType: 'LOCAL',
      framework: 'vue3',
    },
    'mango-workflow': {
      mode: (import.meta.env.VITE_MANGO_WORKFLOW_MODE || 'local') as any,
      entry: import.meta.env.VITE_MANGO_WORKFLOW_ENTRY || 'http://127.0.0.1:5182/',
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
  const allowedOrigins = splitEnvList(import.meta.env.VITE_MANGO_ALLOWED_REMOTE_ORIGINS);
  const allowedHosts = splitEnvList(import.meta.env.VITE_MANGO_ALLOWED_REMOTE_HOSTS);
  const allowHttpEntries = import.meta.env.VITE_MANGO_ALLOW_HTTP_REMOTE_ENTRIES === 'true' && (!isPrdLike || isE2E);

  if (!isProduction) {
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
