import { mangoAuthAdminCapability } from '@mango/auth-admin/capability';
import { mangoCalendarAdminCapability } from '@mango/calendar-admin/capability';
import { mangoFileAdminCapability } from '@mango/file-admin/capability';
import { mangoNoticeAdminCapability } from '@mango/notice-admin/capability';
import { mangoNumgenAdminCapability } from '@mango/numgen-admin/capability';
import { mangoRbacAdminCapability } from '@mango/rbac-admin/capability';
import { mangoSystemAdminCapability } from '@mango/system-admin/capability';
import { mangoTemplateAdminCapability } from '@mango/template-admin/capability';
import { mangoWorkflowAdminCapability } from '@mango/workflow-admin/capability';
import {
  registerCapabilities,
  registerModulePages,
  registerShellPages,
  type MangoCapabilityManifest,
  type MangoPageRegistry,
  type MangoShellPageLoaders,
} from './core';

let registered = false;
const loadWorkflowBusinessExample = () => import('@mango/workflow-business-example');

export type RegisterDefaultAdminPagesOptions = {
  shellPages?: MangoShellPageLoaders;
  capabilities?: MangoCapabilityManifest[];
  registries?: MangoPageRegistry[];
};

export const mangoDefaultCapabilities: MangoCapabilityManifest[] = [
  mangoAuthAdminCapability,
  mangoRbacAdminCapability,
  mangoSystemAdminCapability,
  mangoTemplateAdminCapability,
  mangoFileAdminCapability,
  mangoNoticeAdminCapability,
  mangoNumgenAdminCapability,
  mangoCalendarAdminCapability,
  mangoWorkflowAdminCapability,
];

export function registerDefaultAdminPages(options: RegisterDefaultAdminPagesOptions = {}) {
  if (registered) {
    return;
  }
  registered = true;
  void loadWorkflowBusinessExample()
    .then(m => m.registerWorkflowBusinessExampleComponents());

  registerShellPages(options.shellPages || {});
  registerCapabilities(options.capabilities || mangoDefaultCapabilities);
  registerModulePages({
    moduleCode: 'mango-workflow',
    pages: {
      'workflow/business-form/index': () => loadWorkflowBusinessExample().then(m => m.WorkflowBusinessFormView),
    },
  });
  (options.registries || []).forEach(registerModulePages);
}
