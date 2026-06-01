declare module '@mango/admin' {
  export type MangoAdminFeatureCode =
    | 'authorization'
    | 'system'
    | 'workflow'
    | 'file'
    | 'template'
    | 'notice'
    | 'numgen'
    | 'calendar';

  export type MangoAdminFeatures =
    | 'core'
    | 'full'
    | readonly MangoAdminFeatureCode[]
    | Partial<Record<MangoAdminFeatureCode, boolean>>;

  export interface MangoAdminFeatureRegistrar {
    (): void | Promise<void>;
  }

  export interface MangoAdminShellOptions {
    mountTarget?: string | Element;
    apiBaseUrl?: string;
    title?: string;
    features?: MangoAdminFeatures;
    featureRegistrars?: MangoAdminFeatureRegistrar[];
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
    mount: (target?: string | Element) => unknown;
  }

  export function createMangoAdminApp(options?: MangoAdminShellOptions): MangoAdminAppInstance;
}

declare module '@mango/workflow/admin-pages' {
  export function registerMangoWorkflowAdminPages(): void | Promise<void>;
}

declare module '@mango/workflow-business-example/admin-pages' {
  export function registerMangoWorkflowBusinessExampleAdminPages(): void | Promise<void>;
}

declare module '@mango/template/admin-pages' {
  export function registerMangoTemplateAdminPages(): void | Promise<void>;
}

declare module '@mango/notice/admin-pages' {
  export function registerMangoNoticeAdminPages(): void | Promise<void>;
}

declare module '@mango/notice/admin-shell' {
  export function registerMangoNoticeAdminShell(): void | Promise<void>;
}

declare module '@mango/file/admin-pages' {
  export function registerMangoFileAdminPages(): void | Promise<void>;
}

declare module '@mango/numgen/admin-pages' {
  export function registerMangoNumgenAdminPages(): void | Promise<void>;
}

declare module '@mango/calendar/admin-pages' {
  export function registerMangoCalendarAdminPages(): void | Promise<void>;
}
