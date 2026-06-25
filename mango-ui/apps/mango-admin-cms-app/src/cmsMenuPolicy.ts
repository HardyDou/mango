import { normalizeComponentPath } from '@mango/admin-pages';
import { MenuTypeEnum, type CmsMenu } from './types';

const deprecatedMenus = new Set([
  'cms:site-setting',
]);

const deprecatedComponents = new Set([
  'cms/site-settings/index',
]);

const deprecatedPaths = new Set([
  '/cms/site-settings',
]);

export function normalizeCmsMenus(source: CmsMenu[], hasComponent: (component?: string) => boolean) {
  return filterVisible(source)
    .map(menu => pickCmsBranch(menu, hasComponent))
    .filter(Boolean) as CmsMenu[];
}

export function isDeprecatedCmsMenu(menu?: CmsMenu) {
  if (!menu) {
    return false;
  }
  const component = normalizeComponentPath(menu.component);
  return deprecatedMenus.has(menu.menuCode)
    || deprecatedPaths.has(menu.path || '')
    || deprecatedComponents.has(component);
}

function filterVisible(source: CmsMenu[]): CmsMenu[] {
  return source
    .filter(menu => menu.menuType !== MenuTypeEnum.BUTTON && menu.visible !== 0 && !isDeprecatedCmsMenu(menu))
    .map(menu => ({
      ...menu,
      children: menu.children ? filterVisible(menu.children) : [],
    }));
}

function pickCmsBranch(menu: CmsMenu, hasComponent: (component?: string) => boolean): CmsMenu | undefined {
  const children = (menu.children || [])
    .map(child => pickCmsBranch(child, hasComponent))
    .filter(Boolean) as CmsMenu[];
  const isCmsPage = hasComponent(menu.component);
  if (!isCmsPage && children.length === 0) {
    return undefined;
  }
  return {
    ...menu,
    children,
  };
}
