import type { App as VueApp } from 'vue';
import type { Router } from 'vue-router';
import type {
  MangoFrontendApp,
  MangoRuntimeConfig,
  MangoRuntimeConfigLoadOptions,
} from '@mango/app-runtime';
import type { MangoAdminFeatures } from '@mango/admin-pages/features';
import type { MangoAuthConfig } from '@mango/auth';

export type MangoAdminFeatureRegistrar = () => void | Promise<void>;

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
