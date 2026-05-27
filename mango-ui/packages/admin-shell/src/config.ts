import type { MangoAuthConfig } from '@mango/auth';
import type { MangoFrontendApp, MangoRuntimeConfig, MangoRuntimeConfigLoadOptions } from '@mango/app-runtime';

export interface MangoAdminShellOptions {
  mountTarget?: string | Element;
  apiBaseUrl?: string;
  title?: string;
  login?: MangoAuthConfig['login'];
  modules?: MangoRuntimeConfig['modules'];
  localApps?: MangoFrontendApp[];
  runtimeConfigUrl?: string;
  runtimeConfigLoadOptions?: Partial<MangoRuntimeConfigLoadOptions>;
}

export const defaultMangoAdminShellOptions: Required<Pick<MangoAdminShellOptions, 'mountTarget' | 'apiBaseUrl' | 'title'>> = {
  mountTarget: '#app',
  apiBaseUrl: '/api',
  title: 'Mango Admin',
};

let mangoAdminShellOptions: MangoAdminShellOptions = {
  ...defaultMangoAdminShellOptions,
};

export function configureMangoAdminShell(options: MangoAdminShellOptions = {}) {
  mangoAdminShellOptions = {
    ...mangoAdminShellOptions,
    ...options,
    login: {
      ...mangoAdminShellOptions.login,
      ...options.login,
      brand: {
        ...mangoAdminShellOptions.login?.brand,
        ...options.login?.brand,
      },
      defaults: {
        ...mangoAdminShellOptions.login?.defaults,
        ...options.login?.defaults,
      },
      slots: {
        ...mangoAdminShellOptions.login?.slots,
        ...options.login?.slots,
      },
    },
    modules: {
      ...mangoAdminShellOptions.modules,
      ...options.modules,
    },
    runtimeConfigLoadOptions: {
      ...mangoAdminShellOptions.runtimeConfigLoadOptions,
      ...options.runtimeConfigLoadOptions,
    },
  };
  return mangoAdminShellOptions;
}

export function getMangoAdminShellOptions() {
  return mangoAdminShellOptions;
}
