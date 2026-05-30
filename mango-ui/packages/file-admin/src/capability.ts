import { mangoFileCapability as baseFileCapability, mangoFilePageRegistry as baseFilePageRegistry } from '@mango/file/capability';
import type { MangoCapabilityManifest, MangoCapabilityMenu, MangoCapabilityPage, MangoPageRegistry } from '@mango/admin-pages/core';

export const mangoFileAdminPageRegistry: MangoPageRegistry = {
  ...baseFilePageRegistry,
};

export const mangoFileAdminCapability: MangoCapabilityManifest = {
  ...baseFileCapability,
  packageName: '@mango/file-admin',
  pages: baseFileCapability.pages.map((page: MangoCapabilityPage) => ({ ...page })),
  menus: (baseFileCapability.menus || []).map((menu: MangoCapabilityMenu) => ({ ...menu })),
  permissions: [...(baseFileCapability.permissions || [])],
  styles: [...(baseFileCapability.styles || [])],
};

export { mangoFileAdminCapability as mangoFileCapability, mangoFileAdminPageRegistry as mangoFilePageRegistry };
