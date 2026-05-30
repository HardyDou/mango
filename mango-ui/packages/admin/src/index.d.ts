import type { App as VueApp } from 'vue';
import type { Router } from 'vue-router';

export interface MangoAdminAppInstance {
  app: VueApp;
  router: Router;
  mount: (target?: string | Element) => unknown;
}

export interface MangoAuthBrandConfig {
  title?: string;
  subtitle?: string;
  logo?: string;
}

export interface MangoAuthLoginDefaults {
  tenantCode?: string;
  realm?: string;
  actorType?: string;
  partyType?: string;
  appCode?: string;
  redirectPath?: string;
}

export interface MangoAuthConfig {
  login?: {
    brand?: MangoAuthBrandConfig;
    defaults?: MangoAuthLoginDefaults;
  };
  profile?: {
    roleLabel?: string;
  };
  password?: {
    minLength?: number;
  };
}

export type MangoAdminContentMode = 'router-view' | 'runtime-outlet';

export interface MangoAdminShellOptions {
  mountTarget?: string | Element;
  apiBaseUrl?: string;
  title?: string;
  contentMode?: MangoAdminContentMode;
  auth?: MangoAuthConfig;
  devCenter?: {
    visible?: boolean;
  };
}

export declare function createMangoAdminApp(options?: MangoAdminShellOptions): MangoAdminAppInstance;
