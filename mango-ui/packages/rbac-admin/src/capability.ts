import { mangoRbacCapability as baseRbacCapability, mangoRbacPageRegistry as baseRbacPageRegistry } from '@mango/rbac/capability';
import type { MangoCapabilityManifest, MangoCapabilityMenu, MangoCapabilityPage, MangoPageRegistry } from '@mango/admin-pages/core';

export const mangoRbacAdminPageRegistry: MangoPageRegistry = {
  ...baseRbacPageRegistry,
};

export const mangoRbacAdminCapability: MangoCapabilityManifest = {
  ...baseRbacCapability,
  packageName: '@mango/rbac-admin',
  pages: baseRbacCapability.pages.map((page: MangoCapabilityPage) => ({ ...page })),
  menus: (baseRbacCapability.menus || []).map((menu: MangoCapabilityMenu) => ({ ...menu })),
  permissions: [...(baseRbacCapability.permissions || [])],
  styles: [...(baseRbacCapability.styles || [])],
};

export { mangoRbacAdminCapability as mangoRbacCapability, mangoRbacAdminPageRegistry as mangoRbacPageRegistry };
