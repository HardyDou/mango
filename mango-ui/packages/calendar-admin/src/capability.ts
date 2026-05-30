import { mangoCalendarCapability as baseCalendarCapability, mangoCalendarPageRegistry as baseCalendarPageRegistry } from '@mango/calendar/capability';
import type { MangoCapabilityManifest, MangoCapabilityMenu, MangoCapabilityPage, MangoPageRegistry } from '@mango/admin-pages/core';

export const mangoCalendarAdminPageRegistry: MangoPageRegistry = {
  ...baseCalendarPageRegistry,
};

export const mangoCalendarAdminCapability: MangoCapabilityManifest = {
  ...baseCalendarCapability,
  packageName: '@mango/calendar-admin',
  pages: baseCalendarCapability.pages.map((page: MangoCapabilityPage) => ({ ...page })),
  menus: (baseCalendarCapability.menus || []).map((menu: MangoCapabilityMenu) => ({ ...menu })),
  permissions: [...(baseCalendarCapability.permissions || [])],
  styles: [...(baseCalendarCapability.styles || [])],
};

export { mangoCalendarAdminCapability as mangoCalendarCapability, mangoCalendarAdminPageRegistry as mangoCalendarPageRegistry };
