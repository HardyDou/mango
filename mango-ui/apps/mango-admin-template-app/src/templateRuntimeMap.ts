import { getPageLoader, normalizeComponentPath } from '@mango/admin-pages';

export const TEMPLATE_MODULE_CODE = 'mango-template';

export function resolveTemplateComponent(componentPath?: string) {
  return getPageLoader(TEMPLATE_MODULE_CODE, normalizeComponentPath(componentPath));
}
