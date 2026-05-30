export type MangoFrontendAppType = 'LOCAL' | 'MICRO_APP' | 'IFRAME' | 'EXTERNAL_LINK';
export type MangoDeployMode = 'EMBEDDED' | 'REMOTE' | 'HYBRID';
export type MangoStyleIsolation = 'NONE' | 'SCOPED' | 'SHADOW_DOM' | 'IFRAME';
export type MangoMenuPageType = 'LOCAL_ROUTE' | 'MICRO_ROUTE' | 'IFRAME' | 'EXTERNAL_LINK' | 'BUTTON';
export type MangoRuntimeProfile = 'monolith' | 'hybrid' | 'micro';
export type MangoModuleRuntimeMode = 'local' | 'micro';

export interface MangoModuleRuntimeStrategy {
  moduleCode?: string;
  mode: MangoModuleRuntimeMode;
  entry?: string;
  style?: string;
  runtimeCode?: string;
  appType?: MangoFrontendAppType;
  framework?: string;
  pageType?: MangoMenuPageType;
  deployProfile?: MangoRuntimeProfile | string;
  timeoutMs?: number;
  preload?: boolean;
  alive?: boolean;
}
