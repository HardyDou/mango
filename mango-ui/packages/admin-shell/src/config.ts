import type { Component } from 'vue';
import type { MangoAuthConfig } from '@mango/auth';
import type { MangoFrontendApp, MangoRuntimeConfig, MangoRuntimeConfigLoadOptions, MangoRuntimeTheme } from '@mango/app-runtime';
import type { ShellMenu } from './runtime/menuHost';
import type { ShellMenuMergeReport } from './runtime/menuMerge';

export interface MangoAdminShellComponentOptions {
  app?: Component;
  shell?: Component;
  layout?: Component;
  login?: Component;
  notFound?: Component;
  profile?: Component;
  password?: Component;
}

export interface MangoAdminShellMenuLoaderContext {
  appCode: string;
}

export interface MangoAdminShellMenuOptions {
  appCode?: string;
  loader?: (context: MangoAdminShellMenuLoaderContext) => Promise<ShellMenu[]>;
  capabilityMenus?: ShellMenu[];
  businessMenus?: ShellMenu[];
  permissions?: string[];
  onMergeReport?: (report: ShellMenuMergeReport) => void;
}

export interface MangoAdminShellLayoutOptions {
  defaultLayout?: 'defaults' | 'classic' | 'transverse' | 'columns';
  showLogo?: boolean;
  showBreadcrumb?: boolean;
  showTagsView?: boolean;
}

export interface MangoAdminShellOptions {
  mountTarget?: string | Element;
  apiBaseUrl?: string;
  title?: string;
  login?: MangoAuthConfig['login'];
  profile?: MangoAuthConfig['profile'];
  password?: MangoAuthConfig['password'];
  components?: MangoAdminShellComponentOptions;
  theme?: MangoRuntimeTheme;
  layout?: MangoAdminShellLayoutOptions;
  menu?: MangoAdminShellMenuOptions;
  modules?: MangoRuntimeConfig['modules'];
  localApps?: MangoFrontendApp[];
  runtimeConfigUrl?: string;
  runtimeConfigLoadOptions?: Partial<MangoRuntimeConfigLoadOptions>;
  runtimeDebug?: boolean;
}

export const defaultMangoAdminShellOptions: Required<Pick<MangoAdminShellOptions, 'mountTarget' | 'apiBaseUrl' | 'title'>> = {
  mountTarget: '#app',
  apiBaseUrl: '/api',
  title: 'Mango Admin',
};

let mangoAdminShellOptions: MangoAdminShellOptions = {
  ...defaultMangoAdminShellOptions,
};

export function configureMangoAdminShell(options: MangoAdminShellOptions = {}) {
	mangoAdminShellOptions = {
	  ...mangoAdminShellOptions,
	  ...options,
	  components: {
	    ...mangoAdminShellOptions.components,
	    ...options.components,
	  },
	  login: {
	    ...mangoAdminShellOptions.login,
	    ...options.login,
      brand: {
        ...mangoAdminShellOptions.login?.brand,
        ...options.login?.brand,
      },
      defaults: {
        ...mangoAdminShellOptions.login?.defaults,
        ...options.login?.defaults,
      },
      slots: {
        ...mangoAdminShellOptions.login?.slots,
	      ...options.login?.slots,
	    },
	  },
	  profile: {
	    ...mangoAdminShellOptions.profile,
	    ...options.profile,
	    slots: {
	      ...mangoAdminShellOptions.profile?.slots,
	      ...options.profile?.slots,
	    },
	  },
	  password: {
	    ...mangoAdminShellOptions.password,
	    ...options.password,
	    slots: {
	      ...mangoAdminShellOptions.password?.slots,
	      ...options.password?.slots,
	    },
	  },
	  theme: {
	    ...mangoAdminShellOptions.theme,
	    ...options.theme,
	    tokens: {
	      ...mangoAdminShellOptions.theme?.tokens,
	      ...options.theme?.tokens,
	    },
	  },
	  layout: {
	    ...mangoAdminShellOptions.layout,
	    ...options.layout,
	  },
	  menu: {
	    ...mangoAdminShellOptions.menu,
	    ...options.menu,
	  },
	  modules: {
	    ...mangoAdminShellOptions.modules,
	    ...options.modules,
    },
    runtimeConfigLoadOptions: {
      ...mangoAdminShellOptions.runtimeConfigLoadOptions,
      ...options.runtimeConfigLoadOptions,
    },
  };
  return mangoAdminShellOptions;
}

export function getMangoAdminShellOptions() {
  return mangoAdminShellOptions;
}
