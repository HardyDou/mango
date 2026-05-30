import {
  getPageLoader,
  normalizeComponentPath,
  registerDefaultAdminPages,
} from '@mango/admin-pages';
import { registerMangoAdminShellDevPages } from '@mango/admin-shell/dev-pages';

registerDefaultAdminPages({
  shellPages: {
    home: () => import('@/views/home/index.vue'),
    notFound: () => import('@/views/error/404.vue'),
  },
});

if (import.meta.env.DEV) {
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
