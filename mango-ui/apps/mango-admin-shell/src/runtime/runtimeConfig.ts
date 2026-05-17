import {
  loadRuntimeConfig,
  type MangoRuntimeConfig,
} from '@mango/app-runtime';

export const defaultRuntimeConfig: MangoRuntimeConfig = {
  profile: (import.meta.env.VITE_MANGO_RUNTIME_PROFILE || 'monolith') as MangoRuntimeConfig['profile'],
  modules: {
    'mango-authorization': {
      mode: (import.meta.env.VITE_MANGO_RBAC_MODE || 'local') as any,
      entry: import.meta.env.VITE_MANGO_RBAC_ENTRY || 'http://127.0.0.1:5181/src/micro.ts',
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
      entry: import.meta.env.VITE_MANGO_WORKFLOW_ENTRY || 'http://127.0.0.1:5182/src/micro.ts',
      runtimeCode: 'mango-admin-workflow-app',
      appType: 'MICRO_APP',
      framework: 'vue3',
    },
  },
};

export function loadShellRuntimeConfig() {
  return loadRuntimeConfig(defaultRuntimeConfig);
}
