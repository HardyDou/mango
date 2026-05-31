import type { App as VueApp } from 'vue';
import type { Router } from 'vue-router';
import type {
  MangoFrontendApp,
  MangoRuntimeConfig,
  MangoRuntimeConfigLoadOptions,
} from '@mango/app-runtime';
import type { MangoAuthConfig } from '@mango/auth';

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
  runtimeConfigUrl?: string;
  runtimeConfigLoadOptions?: Partial<MangoRuntimeConfigLoadOptions>;
}

export declare function createMangoAdminApp(options?: MangoAdminShellOptions): MangoAdminAppInstance;
