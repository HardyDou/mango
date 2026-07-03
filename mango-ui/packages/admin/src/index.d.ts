import type { App as VueApp } from 'vue';
import type { Router } from 'vue-router';
import type {
  MangoFrontendApp,
  MangoRuntimeConfig,
  MangoRuntimeConfigLoadOptions,
} from '@mango/app-runtime';
import type { MangoAdminFeatureCode, MangoAdminFeatures } from '@mango/admin-pages/features';
import type { MangoAuthConfig } from '@mango/auth';
import type { MangoGridWidgetDefinition } from '@mango/grid-widgets';

export interface MangoAdminFeatureRegistration {
  businessDomainCode?: string;
  businessDomainName?: string;
  groupName?: string;
  moduleCode?: string;
  moduleName?: string;
  widgets?: MangoGridWidgetDefinition[];
}

export type MangoAdminFeatureRegistrar = () =>
  void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
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

export interface MangoAdminAppInstance {
  app: VueApp;
  router: Router;
  mount: (target?: string | Element) => unknown;
}

export type MangoAdminContentMode = 'router-view' | 'runtime-outlet';
export type MangoAdminShellDeployEnv = 'dev' | 'test' | 'prod' | 'prd' | 'production' | string;

export interface MangoAdminShellDevCenterOptions {
  visible?: boolean;
  deployEnv?: MangoAdminShellDeployEnv;
  registrars?: MangoAdminDevCenterRegistrar[];
  pages?: () => MangoAdminDevCenterPage[];
}

export interface MangoAdminShellOptions {
  mountTarget?: string | Element;
  apiBaseUrl?: string;
  title?: string;
  contentMode?: MangoAdminContentMode;
  devCenter?: MangoAdminShellDevCenterOptions;
  login?: MangoAuthConfig['login'];
  modules?: MangoRuntimeConfig['modules'];
  localApps?: MangoFrontendApp[];
  features?: MangoAdminFeatures;
  featureRegistrars?: MangoAdminFeatureRegistrar[];
  runtimeConfigUrl?: string;
  runtimeConfigLoadOptions?: Partial<MangoRuntimeConfigLoadOptions>;
}

export declare function createMangoAdminApp(options?: MangoAdminShellOptions): MangoAdminAppInstance;
