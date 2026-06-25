import { getPageLoader, normalizeComponentPath } from '@mango/admin-pages';

export const CMS_MODULE_CODE = 'mango-cms';

export function resolveCmsComponent(componentPath?: string) {
  return getPageLoader(CMS_MODULE_CODE, normalizeComponentPath(componentPath));
}
