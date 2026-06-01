import { createMangoAdminApp, mangoFullAdminFeatureRegistrars } from '@mango/admin/full';
import '@mango/admin/style-full.css';

function splitEnvList(value?: string) {
  return (value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: import.meta.env.VITE_MANGO_API_BASE_URL || '/api',
  title: import.meta.env.VITE_APP_TITLE || 'NexusGeneratedFull',
  features: 'full',
  featureRegistrars: mangoFullAdminFeatureRegistrars,
  devCenter: {
    deployEnv: import.meta.env.VITE_MANGO_DEPLOY_ENV || import.meta.env.MODE,
  },
  runtimeConfigLoadOptions: import.meta.env.DEV
    ? {
        failClosed: false,
        allowHttpEntries: true,
        allowRelativeEntries: true,
        allowedEntryOrigins: splitEnvList(import.meta.env.VITE_MANGO_ALLOWED_REMOTE_ORIGINS),
        allowedEntryHosts: splitEnvList(import.meta.env.VITE_MANGO_ALLOWED_REMOTE_HOSTS),
      }
    : undefined,
}).mount();
