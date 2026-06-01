import type { MangoAuthConfig } from '@mango/auth';
import type { MangoFrontendApp, MangoRuntimeConfig, MangoRuntimeConfigLoadOptions } from '@mango/app-runtime';
import type { MangoAdminFeatureCode, MangoAdminFeatures } from '@mango/admin-pages/features';

export type MangoAdminFeatureRegistrar = () => void | Promise<void>;
export type MangoAdminDevCenterRegistrar = () => void | Promise<void>;

export interface MangoAdminDevCenterPage {
  menuId: string;
  menuName: string;
  menuCode: string;
  path: string;
  component: string;
  icon: string;
  sort: number;
  feature?: MangoAdminFeatureCode;
}

export interface MangoAdminShellOptions {
  mountTarget?: string | Element;
  apiBaseUrl?: string;
  title?: string;
  contentMode?: 'router-view' | 'runtime-outlet';
  devCenter?: MangoAdminShellDevCenterOptions;
  login?: MangoAuthConfig['login'];
  modules?: MangoRuntimeConfig['modules'];
  localApps?: MangoFrontendApp[];
  features?: MangoAdminFeatures;
  featureRegistrars?: MangoAdminFeatureRegistrar[];
  runtimeConfigUrl?: string;
  runtimeConfigLoadOptions?: Partial<MangoRuntimeConfigLoadOptions>;
}

export type MangoAdminShellDeployEnv = 'dev' | 'test' | 'prod' | 'prd' | 'production' | string;

export interface MangoAdminShellDevCenterOptions {
  visible?: boolean;
  deployEnv?: MangoAdminShellDeployEnv;
  registrars?: MangoAdminDevCenterRegistrar[];
  pages?: () => MangoAdminDevCenterPage[];
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
    devCenter: {
      ...mangoAdminShellOptions.devCenter,
      ...options.devCenter,
      registrars: options.devCenter?.registrars || mangoAdminShellOptions.devCenter?.registrars,
      pages: options.devCenter?.pages || mangoAdminShellOptions.devCenter?.pages,
    },
    modules: {
      ...mangoAdminShellOptions.modules,
      ...options.modules,
    },
    featureRegistrars: options.featureRegistrars || mangoAdminShellOptions.featureRegistrars,
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
