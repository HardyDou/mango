import { reactive, type App, type Component, type InjectionKey } from 'vue';

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

export const mangoAuthConfigKey: InjectionKey<MangoAuthConfig> = Symbol('mangoAuthConfig');

const globalMangoAuthConfig = reactive<MangoAuthConfig>({});

export function installMangoAuth(app?: App, config: MangoAuthConfig = {}) {
  Object.assign(globalMangoAuthConfig, mergeAuthConfig(globalMangoAuthConfig, config));
  app?.provide(mangoAuthConfigKey, globalMangoAuthConfig);
}

export function getMangoAuthConfig() {
  return globalMangoAuthConfig;
}

export function mergeAuthConfig(base: MangoAuthConfig, override: MangoAuthConfig): MangoAuthConfig {
  return {
    ...base,
    ...override,
    login: {
      ...base.login,
      ...override.login,
      brand: {
        ...base.login?.brand,
        ...override.login?.brand,
      },
      slots: {
        ...base.login?.slots,
        ...override.login?.slots,
      },
      defaults: {
        ...base.login?.defaults,
        ...override.login?.defaults,
      },
    },
    profile: {
      ...base.profile,
      ...override.profile,
      slots: {
        ...base.profile?.slots,
        ...override.profile?.slots,
      },
    },
    password: {
      ...base.password,
      ...override.password,
      slots: {
        ...base.password?.slots,
        ...override.password?.slots,
      },
    },
  };
}
