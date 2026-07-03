import {
  getPageLoader,
  normalizeComponentPath,
  registerDefaultAdminPages,
} from '@mango/admin-pages';
import {
  configureMangoAdminShell,
  ensureFeatureRegistrars,
} from '@mango/admin-shell';
import { registerMangoAdminShellBaseDevPages } from '@mango/admin-shell/dev-base-pages';
import { registerMangoAdminShellDevPages } from '@mango/admin-shell/dev-pages';
import { mangoFullAdminFeatureRegistrars } from '@mango/admin/full';
import { systemQuickEntryWidgets, systemUserProfileWidgets } from '@mango/system';

configureMangoAdminShell({
  features: 'full',
  featureRegistrars: mangoFullAdminFeatureRegistrars,
  widgets: [
    ...systemUserProfileWidgets,
    ...systemQuickEntryWidgets,
  ],
});

void ensureFeatureRegistrars().catch((error) => {
  console.error('[mango-admin] failed to register admin feature modules', error);
});

registerDefaultAdminPages({
  features: 'full',
  shellPages: {
    home: () => import('@/views/home/index.vue'),
    notFound: () => import('@/views/error/404.vue'),
  },
  registries: [{
    moduleCode: 'mango-shell',
    pages: {
      'home/management/index': () => import('@mango/admin-shell/home-management'),
      'home/templates/index': () => import('@mango/admin-shell/home-templates'),
      'home/list/index': () => import('@mango/admin-shell/home-list'),
      'home/user/index': () => import('@mango/admin-shell/home-user'),
    },
  }],
});
if (import.meta.env.DEV) {
  registerMangoAdminShellBaseDevPages();
  registerMangoAdminShellDevPages();
}

export const componentsMap: Record<string, any> = new Proxy({}, {
  get(_target, key: string) {
    return getPageLoader(undefined, key);
  },
  has(_target, key: string) {
    return Boolean(getPageLoader(undefined, key));
  },
});

export { normalizeComponentPath };
