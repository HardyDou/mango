// mango-cli:imports:start
{{frontendEntryImports}}
import type { MangoAdminFeatureRegistrar } from '@mango/admin';
// mango-cli:imports:end

function splitEnvList(value?: string) {
  return (value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

// mango-cli:features:start
const mangoFeatures = {{frontendFeaturesExpression}};
const mangoFeatureRegistrars: MangoAdminFeatureRegistrar[] = {{frontendFeatureRegistrarsExpression}};
// mango-cli:features:end

// mango-cli:business-feature-registrars:start
const mangoBusinessFeatureRegistrars: MangoAdminFeatureRegistrar[] = [
];
// mango-cli:business-feature-registrars:end

const mangoAllFeatureRegistrars: MangoAdminFeatureRegistrar[] = [
  ...mangoFeatureRegistrars,
  ...mangoBusinessFeatureRegistrars,
];

createMangoAdminApp({
  mountTarget: '#app',
  apiBaseUrl: import.meta.env.VITE_MANGO_API_BASE_URL || '/api',
  title: import.meta.env.VITE_APP_TITLE || '{{projectPascal}}',
  features: mangoFeatures,
  featureRegistrars: mangoAllFeatureRegistrars,
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
