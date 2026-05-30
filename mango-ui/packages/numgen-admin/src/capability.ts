import { mangoNumgenCapability as baseNumgenCapability, mangoNumgenPageRegistry as baseNumgenPageRegistry } from '@mango/numgen/capability';
import type { MangoCapabilityManifest, MangoCapabilityMenu, MangoCapabilityPage, MangoPageRegistry } from '@mango/admin-pages/core';

export const mangoNumgenAdminPageRegistry: MangoPageRegistry = {
  ...baseNumgenPageRegistry,
};

export const mangoNumgenAdminCapability: MangoCapabilityManifest = {
  ...baseNumgenCapability,
  packageName: '@mango/numgen-admin',
  pages: baseNumgenCapability.pages.map((page: MangoCapabilityPage) => ({ ...page })),
  menus: (baseNumgenCapability.menus || []).map((menu: MangoCapabilityMenu) => ({ ...menu })),
  permissions: [...(baseNumgenCapability.permissions || [])],
  styles: [...(baseNumgenCapability.styles || [])],
};

export { mangoNumgenAdminCapability as mangoNumgenCapability, mangoNumgenAdminPageRegistry as mangoNumgenPageRegistry };
