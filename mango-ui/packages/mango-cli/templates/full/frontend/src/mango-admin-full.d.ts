declare module '@mango/admin/full' {
  import type { App as VueApp } from 'vue';
  import type { Router } from 'vue-router';

  export interface MangoAdminFeatureRegistrar {
    (): void | Promise<void>;
  }

  export interface MangoAdminShellOptions {
    mountTarget?: string | Element;
    apiBaseUrl?: string;
    title?: string;
    features?: 'full' | string[] | Set<string>;
    featureRegistrars?: MangoAdminFeatureRegistrar[];
    devCenter?: {
      deployEnv?: string;
      enabled?: boolean;
    };
    runtimeConfigLoadOptions?: {
      failClosed?: boolean;
      allowHttpEntries?: boolean;
      allowRelativeEntries?: boolean;
      allowedEntryOrigins?: string[];
      allowedEntryHosts?: string[];
    };
  }

  export interface MangoAdminAppInstance {
    app: VueApp;
    router: Router;
    mount: (target?: string | Element) => unknown;
  }

  export const mangoFullAdminFeatureRegistrars: MangoAdminFeatureRegistrar[];
  export function createMangoAdminApp(options?: MangoAdminShellOptions): MangoAdminAppInstance;
}
