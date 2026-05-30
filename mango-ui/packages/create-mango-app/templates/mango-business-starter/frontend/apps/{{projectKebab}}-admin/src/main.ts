import { createMangoAdmin } from '@mango/admin';
import '@mango/admin/style.css';
import { register{{modulePascal}}Pages } from '@{{projectKebab}}/{{moduleKebab}}-admin';
import { starterRuntimeConfig } from './runtimeConfig';
import { starterMenus } from './starterMenus';
{{mangoFeatureImports}}

(globalThis as any).__MANGO_IMPORT_META_ENV__ = import.meta.env;

const enabledMangoCapabilities = [
{{mangoFeatureCapabilities}},
];

register{{modulePascal}}Pages();

createMangoAdmin({
  preset: '{{adminPreset}}',
  mountTarget: '#app',
  apiBaseUrl: import.meta.env.VITE_MANGO_API_BASE_URL || '/api',
  title: import.meta.env.VITE_APP_TITLE || '{{projectPascal}} Admin',
  capabilities: enabledMangoCapabilities,
  modules: starterRuntimeConfig.modules,
  runtimeConfigUrl: import.meta.env.VITE_MANGO_RUNTIME_CONFIG_URL || '/mango-runtime-config.json',
  runtimeConfigLoadOptions: {
    allowHttpEntries: import.meta.env.DEV || import.meta.env.VITE_MANGO_ALLOW_HTTP_REMOTE_ENTRIES === 'true',
    allowedEntryOrigins: ['http://127.0.0.1:5190', 'http://localhost:5190'],
  },
  menu: {
    appCode: 'internal-admin',
    businessMenus: starterMenus,
  },
}).mount();
