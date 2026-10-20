import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';
import { getRegisteredModulePagesSnapshot, getRegisteredPageRoutes } from '@mango/admin-extension/core';
import { registerMangoInformationAdminPages } from '../admin-pages';

const MENU_DECLARATION = new URL(
  '../../../../../mango/mango-platform/mango-information/mango-information-starter/src/main/resources/META-INF/mango/resources/information-common-menu.json',
  import.meta.url,
);

interface DeclaredMenu {
  menuType?: number;
  menuCode?: string;
  path?: string;
  component?: string;
  visible?: number;
  apiCodes?: string[];
  children?: DeclaredMenu[];
}

function declaredMenus(): DeclaredMenu[] {
  const document = JSON.parse(readFileSync(MENU_DECLARATION, 'utf8')) as {
    mango: { resource: { declarations: { AUTH_MENU: Array<{ fields: { menus: { value: DeclaredMenu[] } } }> } } };
  };
  return document.mango.resource.declarations.AUTH_MENU.flatMap((entry) => entry.fields.menus.value);
}

function flatten(menus: DeclaredMenu[]): DeclaredMenu[] {
  return menus.flatMap((menu) => [menu, ...flatten(menu.children ?? [])]);
}

describe('information admin routes are configuration driven', () => {
  it('registers every page key referenced by the AUTH_MENU declaration', () => {
    registerMangoInformationAdminPages();
    const pages = new Set(getRegisteredModulePagesSnapshot('mango-information')[0]?.pages ?? []);
    const declaredComponents = flatten(declaredMenus())
      .map((menu) => menu.component)
      .filter((component): component is string => Boolean(component));

    expect(declaredComponents.length).toBeGreaterThan(0);
    declaredComponents.forEach((component) => {
      expect(pages.has(component)).toBe(true);
    });
  });

  it('does not register package routes, so routing stays menu driven', () => {
    registerMangoInformationAdminPages();

    expect(getRegisteredPageRoutes(['mango-information'])).toEqual([]);
  });

  it('declares detail, config and submission handling as hidden static routes', () => {
    const hiddenPages = flatten(declaredMenus()).filter((menu) => menu.visible === 0 && menu.component);

    expect(hiddenPages.map((menu) => menu.menuCode).sort()).toEqual([
      'information:form-config',
      'information:form-detail',
      'information:form-submissions',
    ]);
    hiddenPages.forEach((menu) => {
      expect(menu.path).toMatch(/^\/information\/[a-z-]+$/u);
      expect(menu.path).not.toContain(':');
      expect(menu.apiCodes?.length ?? 0).toBeGreaterThan(0);
    });
    expect(hiddenPages.map((menu) => menu.path).sort()).toEqual([
      '/information/form-config',
      '/information/form-detail',
      '/information/form-submissions',
    ]);
  });
});
