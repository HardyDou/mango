import { getPageLoader, normalizeComponentPath } from '@mango/admin-pages';

export const RBAC_MODULE_CODE = 'mango-authorization';

export function resolveRbacComponent(componentPath?: string) {
  return getPageLoader(RBAC_MODULE_CODE, normalizeComponentPath(componentPath));
}
