import type { App, Component } from 'vue';

export interface MangoAuthBrandConfig {
  title?: string;
  subtitle?: string;
  logo?: string;
  panelTitle?: string;
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
    slots?: Record<string, Component>;
  };
  profile?: {
    roleLabel?: string;
    avatarUrl?: string;
    fields?: string[];
    slots?: Record<string, Component>;
  };
  password?: {
    minLength?: number;
    slots?: Record<string, Component>;
  };
}

export declare const LoginView: Component;
export declare const ProfileView: Component;
export declare const PasswordView: Component;
export declare function installMangoAuth(app: App, config?: MangoAuthConfig): void;
export declare function configureMangoAuth(config?: MangoAuthConfig): MangoAuthConfig;
export declare function useAuthConfig(): unknown;
