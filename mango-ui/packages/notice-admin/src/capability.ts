import { mangoNoticeCapability as baseNoticeCapability, mangoNoticePageRegistry as baseNoticePageRegistry } from '@mango/notice/capability';
import type { MangoCapabilityManifest, MangoCapabilityMenu, MangoCapabilityPage, MangoPageRegistry } from '@mango/admin-pages/core';

export const mangoNoticeAdminPageRegistry: MangoPageRegistry = {
  ...baseNoticePageRegistry,
};

export const mangoNoticeAdminCapability: MangoCapabilityManifest = {
  ...baseNoticeCapability,
  packageName: '@mango/notice-admin',
  pages: baseNoticeCapability.pages.map((page: MangoCapabilityPage) => ({ ...page })),
  menus: (baseNoticeCapability.menus || []).map((menu: MangoCapabilityMenu) => ({ ...menu })),
  permissions: [...(baseNoticeCapability.permissions || [])],
  styles: [...(baseNoticeCapability.styles || [])],
};

export { mangoNoticeAdminCapability as mangoNoticeCapability, mangoNoticeAdminPageRegistry as mangoNoticePageRegistry };
