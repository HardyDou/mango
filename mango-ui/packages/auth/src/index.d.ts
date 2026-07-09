import type { App, Component, ComputedRef, Ref } from 'vue';
import type { LoginResult, LoginTenantOption, WecomLoginConfig } from './api/sys';
import type { UserInfosState } from './store/userInfo';

export interface MangoAuthLoginBrandConfig {
  title?: string;
  subtitle?: string;
  panelTitle?: string;
  logoUrl?: string;
  imageUrl?: string;
}

export interface MangoAuthLoginSlots {
  brand?: Component;
  formHeader?: Component;
  formBefore?: Component;
  formAfter?: Component;
  tenantOption?: Component;
  footer?: Component;
}

export interface MangoAuthProfileSlots {
  sidebarTop?: Component;
  sidebarBottom?: Component;
  infoBefore?: Component;
  infoAfter?: Component;
  extraTabs?: Component;
}

export interface MangoAuthPasswordSlots {
  headerExtra?: Component;
  formBefore?: Component;
  formAfter?: Component;
  footer?: Component;
}

export interface MangoAuthConfig {
  login?: {
    brand?: MangoAuthLoginBrandConfig;
    slots?: MangoAuthLoginSlots;
    defaults?: {
      tenantCode?: string;
      realm?: string;
      actorType?: string;
      partyType?: string;
      appCode?: string;
      redirectPath?: string;
      redirectQueryKey?: string;
    };
  };
  profile?: {
    avatarUrl?: string;
    roleLabel?: string;
    fields?: Array<'username' | 'nickname' | 'email' | 'phone' | string>;
    slots?: MangoAuthProfileSlots;
  };
  password?: {
    minLength?: number;
    slots?: MangoAuthPasswordSlots;
  };
}

export interface MangoLoginForm {
  tenantId: string;
  username: string;
  password: string;
}

export interface MangoPasswordResetForm {
  newPassword: string;
  confirmPassword: string;
}

export type MangoLoginDefaults = NonNullable<MangoAuthConfig['login']>['defaults'];

export interface MangoLoginFlowOptions {
  defaults?: MangoLoginDefaults;
  minLoadingMs?: number;
  showMessage?: boolean;
  autoRedirect?: boolean;
}

export interface MangoWecomCallbackState {
  tenantId?: string;
  channelConfigId?: string;
}

export type MangoLoginActionStatus = 'success' | 'password-reset-required' | 'failed' | 'ignored';

export interface MangoLoginActionResult {
  status: MangoLoginActionStatus;
  result?: LoginResult;
  error?: unknown;
}

export interface MangoWecomActionResult extends MangoLoginActionResult {
  shouldOpenWecomDialog: boolean;
}

export interface MangoLoginInitializeResult {
  hasWecomCallback: boolean;
  shouldOpenWecomDialog: boolean;
  result?: MangoWecomActionResult;
}

export declare const LoginView: Component;
export declare const ProfileView: Component;
export declare const PasswordView: Component;
export declare function installMangoAuth(app?: App, config?: MangoAuthConfig): void;
export declare function getMangoAuthConfig(): MangoAuthConfig;
export declare function mergeAuthConfig(base: MangoAuthConfig, override: MangoAuthConfig): MangoAuthConfig;
export declare function useAuthConfig(): ComputedRef<MangoAuthConfig>;
export declare function useUserInfo(): {
  userInfos: UserInfosState['userInfos'];
  setUserInfos(data: Partial<UserInfosState['userInfos']>): void;
  updateTenantInfo(tenantId: string, tenantName: string): void;
  clearUserInfo(): void;
};
export declare function useMangoLoginFlow(options?: MangoLoginFlowOptions): {
  form: MangoLoginForm;
  passwordResetForm: MangoPasswordResetForm;
  loading: Ref<boolean>;
  tenantLoading: Ref<boolean>;
  wecomLoading: Ref<boolean>;
  passwordResetLoading: Ref<boolean>;
  tenantOptions: Ref<LoginTenantOption[]>;
  selectedTenant: ComputedRef<LoginTenantOption | undefined>;
  wecomCode: Ref<string>;
  wecomLoginConfig: Ref<WecomLoginConfig | undefined>;
  wecomQrUrl: ComputedRef<string>;
  passwordResetRequired: Ref<boolean>;
  passwordResetTicket: Ref<string>;
  passwordPolicyMessage: ComputedRef<string>;
  canSubmitPasswordReset: ComputedRef<boolean>;
  lastError: Ref<unknown>;
  loginDefaults: ComputedRef<MangoLoginDefaults>;
  setTenantId(tenantId: string | number | undefined): void;
  setCredentials(credentials: Partial<Pick<MangoLoginForm, 'username' | 'password'>>): void;
  applyTenantOptions(optionsList: LoginTenantOption[], keepExistingTenant?: boolean): void;
  loadLoginTenants(): Promise<LoginTenantOption[]>;
  loadAccountLoginTenants(username?: string): Promise<LoginTenantOption[]>;
  resolveLoginRedirectPath(): string;
  clearPasswordReset(): void;
  submitPasswordLogin(): Promise<MangoLoginActionResult>;
  submitRequiredPasswordChange(): Promise<MangoLoginActionResult>;
  prepareWecomLogin(): Promise<MangoWecomActionResult>;
  submitWecomLogin(code?: string): Promise<MangoWecomActionResult>;
  handleWecomCallback(code: string, state: MangoWecomCallbackState): Promise<MangoWecomActionResult>;
  initializeLoginFlow(): Promise<MangoLoginInitializeResult>;
  buildWecomState(config: WecomLoginConfig): string;
};
export declare function normalizeLoginRedirect(value: unknown): string;
export declare function parseWecomState(rawState: string | null): MangoWecomCallbackState;
export declare function readWecomCallback(): {
  code: string;
  state: MangoWecomCallbackState;
  hasCallbackParams: boolean;
};
export declare function clearWecomCallbackUrl(): void;
export * from './config';
export * from './store/userInfo';
export * from './api/sys';
