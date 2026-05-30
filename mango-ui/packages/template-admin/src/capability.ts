import { mangoTemplateCapability as baseTemplateCapability, mangoTemplatePageRegistry as baseTemplatePageRegistry } from '@mango/template/capability';
import type { MangoCapabilityManifest, MangoCapabilityMenu, MangoCapabilityPage, MangoPageRegistry } from '@mango/admin-pages/core';

export const mangoTemplateAdminPageRegistry: MangoPageRegistry = {
  ...baseTemplatePageRegistry,
};

export const mangoTemplateAdminCapability: MangoCapabilityManifest = {
  ...baseTemplateCapability,
  packageName: '@mango/template-admin',
  pages: baseTemplateCapability.pages.map((page: MangoCapabilityPage) => ({ ...page })),
  menus: (baseTemplateCapability.menus || []).map((menu: MangoCapabilityMenu) => ({ ...menu })),
  permissions: [...(baseTemplateCapability.permissions || [])],
  styles: [...(baseTemplateCapability.styles || [])],
};

export { mangoTemplateAdminCapability as mangoTemplateCapability, mangoTemplateAdminPageRegistry as mangoTemplatePageRegistry };
