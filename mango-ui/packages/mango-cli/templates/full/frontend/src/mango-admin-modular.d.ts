declare module '@mango/admin' {
  import type { App as VueApp } from 'vue';

  export type MangoAdminFeatureCode =
    'authorization' | 'system' | 'workflow' | 'file' | 'template' | 'notice' | 'numgen' | 'calendar';

  export type MangoAdminFeatures =
    'core' | 'full' | readonly MangoAdminFeatureCode[] | Partial<Record<MangoAdminFeatureCode, boolean>>;

  export interface MangoAdminFeatureRegistration {
    businessDomainCode?: string;
    businessDomainName?: string;
    groupName?: string;
    moduleCode?: string;
    moduleName?: string;
    widgets?: unknown[];
  }

  export interface MangoAdminFeatureRegistrar {
    (): void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
  }

  export interface MangoAdminShellOptions {
    mountTarget?: string | Element;
    apiBaseUrl?: string;
    title?: string;
    features?: MangoAdminFeatures;
    featureRegistrars?: MangoAdminFeatureRegistrar[];
    moduleDiagnostics?: {
      enabled?: boolean;
    };
    devCenter?: {
      visible?: boolean;
      deployEnv?: string;
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
    mount: (target?: string | Element) => unknown;
  }

  export interface MangoAdminBootstrapHooks {
    beforeMount?: (instance: MangoAdminAppInstance) => void;
  }

  export function createMangoAdminApp(options?: MangoAdminShellOptions): MangoAdminAppInstance;
  export function bootstrapMangoAdminApp(
    options?: MangoAdminShellOptions,
    hooks?: MangoAdminBootstrapHooks,
  ): MangoAdminAppInstance | undefined;
}

declare module '@mango/job/admin-pages' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoJobAdminPages():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}

declare module '@mango/cms/admin-pages' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoCmsAdminPages():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}

declare module '@mango/link/admin-pages' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoLinkAdminPages():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}

declare module '@mango/workflow/admin-pages' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoWorkflowAdminPages():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}

declare module '@mango/workflow-business-example/admin-pages' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoWorkflowBusinessExampleAdminPages():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}

declare module '@mango/template/admin-pages' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoTemplateAdminPages():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}

declare module '@mango/notice/admin-pages' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoNoticeAdminPages():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}

declare module '@mango/notice/admin-shell' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoNoticeAdminShell():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}

declare module '@mango/file/admin-pages' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoFileAdminPages():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}

declare module '@mango/numgen/admin-pages' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoNumgenAdminPages():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}

declare module '@mango/calendar/admin-pages' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoCalendarAdminPages():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}

declare module '@mango/payment/admin-pages' {
  import type { MangoAdminFeatureRegistration } from '@mango/admin';
  export function registerMangoPaymentAdminPages():
    void | MangoAdminFeatureRegistration | Promise<void | MangoAdminFeatureRegistration>;
}
