// mango-cli:imports:start
{{frontendEntryImports}}
import type { MangoAdminBootstrapHooks, MangoAdminFeatureRegistrar, MangoAdminShellOptions } from '@mango/admin';
import { splitEnvList } from './environment';
// mango-cli:imports:end

// mango-cli:features:start
const mangoFeatures = {{frontendFeaturesExpression}};
const mangoFeatureRegistrars: MangoAdminFeatureRegistrar[] = {{frontendFeatureRegistrarsExpression}};
// mango-cli:features:end

// mango-cli:business-feature-registrars:start
const mangoBusinessFeatureRegistrars: MangoAdminFeatureRegistrar[] = [];
// mango-cli:business-feature-registrars:end

const mangoAllFeatureRegistrars: MangoAdminFeatureRegistrar[] = [
  ...mangoFeatureRegistrars,
  ...mangoBusinessFeatureRegistrars,
];

const mangoAdminOptions: MangoAdminShellOptions = {
  mountTarget: '#app',
  apiBaseUrl: import.meta.env.VITE_MANGO_API_BASE_URL || '/api',
  title: import.meta.env.VITE_APP_TITLE || '{{projectPascal}}',
  features: mangoFeatures,
  featureRegistrars: mangoAllFeatureRegistrars,
  moduleDiagnostics: {
    enabled: import.meta.env.VITE_MANGO_MODULE_DIAGNOSTICS_ENABLED === 'true',
  },
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
};

// mango-cli:bootstrap-hooks:start
const mangoAdminBootstrapHooks: MangoAdminBootstrapHooks = {};
// mango-cli:bootstrap-hooks:end

bootstrapMangoAdminApp(mangoAdminOptions, mangoAdminBootstrapHooks);
