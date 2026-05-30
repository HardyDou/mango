import { mangoSystemCapability as baseSystemCapability, mangoSystemPageRegistry as baseSystemPageRegistry } from '@mango/system/capability';
import type { MangoCapabilityManifest, MangoCapabilityMenu, MangoCapabilityPage, MangoPageRegistry } from '@mango/admin-pages/core';

export const mangoSystemAdminPageRegistry: MangoPageRegistry = {
  ...baseSystemPageRegistry,
};

export const mangoSystemAdminCapability: MangoCapabilityManifest = {
  ...baseSystemCapability,
  packageName: '@mango/system-admin',
  pages: baseSystemCapability.pages.map((page: MangoCapabilityPage) => ({ ...page })),
  menus: (baseSystemCapability.menus || []).map((menu: MangoCapabilityMenu) => ({ ...menu })),
  permissions: [...(baseSystemCapability.permissions || [])],
  styles: [...(baseSystemCapability.styles || [])],
};

export { mangoSystemAdminCapability as mangoSystemCapability, mangoSystemAdminPageRegistry as mangoSystemPageRegistry };
