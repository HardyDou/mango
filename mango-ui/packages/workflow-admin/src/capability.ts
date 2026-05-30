import { mangoWorkflowCapability as baseWorkflowCapability, mangoWorkflowPageRegistry as baseWorkflowPageRegistry } from '@mango/workflow/capability';
import type { MangoCapabilityManifest, MangoCapabilityMenu, MangoCapabilityPage, MangoPageRegistry } from '@mango/admin-pages/core';

export const mangoWorkflowAdminPageRegistry: MangoPageRegistry = {
  ...baseWorkflowPageRegistry,
};

export const mangoWorkflowAdminCapability: MangoCapabilityManifest = {
  ...baseWorkflowCapability,
  packageName: '@mango/workflow-admin',
  pages: baseWorkflowCapability.pages.map((page: MangoCapabilityPage) => ({ ...page })),
  menus: (baseWorkflowCapability.menus || []).map((menu: MangoCapabilityMenu) => ({ ...menu })),
  permissions: [...(baseWorkflowCapability.permissions || [])],
  styles: [...(baseWorkflowCapability.styles || [])],
};

export { mangoWorkflowAdminCapability as mangoWorkflowCapability, mangoWorkflowAdminPageRegistry as mangoWorkflowPageRegistry };
