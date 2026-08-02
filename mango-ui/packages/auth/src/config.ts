import {
  markRaw,
  reactive,
  shallowReadonly,
  shallowRef,
  toRaw,
  type App,
  type Component,
  type InjectionKey,
} from 'vue';

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
  theme?: Component;
}

export interface MangoAuthProfileSection {
  key: string;
  label: string;
  title?: string;
  description?: string;
  group?: string;
  icon?: Component;
  component: Component;
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
    sections?: MangoAuthProfileSection[];
  };
  password?: {
    minLength?: number;
    slots?: MangoAuthPasswordSlots;
  };
}

export const mangoAuthConfigKey: InjectionKey<MangoAuthConfig> = Symbol('mangoAuthConfig');

const globalMangoAuthConfig = reactive<MangoAuthConfig>({});
const registeredProfileSections = shallowRef<MangoAuthProfileSection[]>([]);

export function registerMangoAuthProfileSections(sections: MangoAuthProfileSection[]) {
  const sectionByKey = new Map(registeredProfileSections.value.map((section) => [section.key, section]));
  sections.forEach((section) => {
    sectionByKey.set(section.key, normalizeProfileSection(section));
  });
  registeredProfileSections.value = Array.from(sectionByKey.values());
}

export function getMangoAuthProfileSections() {
  return shallowReadonly(registeredProfileSections);
}

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
      slots: normalizeSlotComponents({
        ...base.login?.slots,
        ...override.login?.slots,
      }),
      defaults: {
        ...base.login?.defaults,
        ...override.login?.defaults,
      },
    },
    profile: {
      ...base.profile,
      ...override.profile,
      slots: normalizeSlotComponents({
        ...base.profile?.slots,
        ...override.profile?.slots,
      }),
      sections: normalizeProfileSections(override.profile?.sections || base.profile?.sections || []),
    },
    password: {
      ...base.password,
      ...override.password,
      slots: normalizeSlotComponents({
        ...base.password?.slots,
        ...override.password?.slots,
      }),
    },
  };
}

function normalizeProfileSections(sections: MangoAuthProfileSection[]) {
  return sections.map(normalizeProfileSection);
}

function normalizeProfileSection(section: MangoAuthProfileSection): MangoAuthProfileSection {
  return {
    ...section,
    icon: normalizeComponent(section.icon),
    component: normalizeComponent(section.component) as Component,
  };
}

function normalizeSlotComponents<T extends Record<string, Component | undefined>>(slots: T): T {
  return Object.fromEntries(
    Object.entries(slots).map(([name, component]) => [name, normalizeComponent(component)]),
  ) as T;
}

function normalizeComponent(component?: Component) {
  return component && (typeof component === 'object' || typeof component === 'function')
    ? markRaw(toRaw(component))
    : component;
}
