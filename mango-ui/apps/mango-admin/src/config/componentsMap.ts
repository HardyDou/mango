import {
  getPageLoader,
  normalizeComponentPath,
  registerDefaultAdminPages,
} from '@mango/admin-pages';

registerDefaultAdminPages();

export const componentsMap: Record<string, any> = new Proxy({}, {
  get(_target, key: string) {
    return getPageLoader(undefined, key);
  },
  has(_target, key: string) {
    return Boolean(getPageLoader(undefined, key));
  },
});

export { normalizeComponentPath };
