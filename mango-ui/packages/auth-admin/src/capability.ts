import { mangoAuthCapability as baseAuthCapability, mangoAuthPageRegistry as baseAuthPageRegistry } from '@mango/auth/capability';
import type { MangoCapabilityManifest, MangoCapabilityMenu, MangoCapabilityPage, MangoPageRegistry } from '@mango/admin-pages/core';

export const mangoAuthAdminPageRegistry: MangoPageRegistry = {
  ...baseAuthPageRegistry,
};

export const mangoAuthAdminCapability: MangoCapabilityManifest = {
  ...baseAuthCapability,
  packageName: '@mango/auth-admin',
  pages: baseAuthCapability.pages.map((page: MangoCapabilityPage) => ({ ...page })),
  menus: (baseAuthCapability.menus || []).map((menu: MangoCapabilityMenu) => ({ ...menu })),
  permissions: [...(baseAuthCapability.permissions || [])],
  styles: [...(baseAuthCapability.styles || [])],
};

export { mangoAuthAdminCapability as mangoAuthCapability, mangoAuthAdminPageRegistry as mangoAuthPageRegistry };
