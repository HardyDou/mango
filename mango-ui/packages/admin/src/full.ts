export { createMangoAdminApp } from '@mango/admin-shell';
export type { MangoAdminShellOptions, MangoAdminAppInstance } from '@mango/admin-shell';
export { registerMangoCalendarAdminPages } from '@mango/calendar/admin-pages';
export { registerMangoFileAdminPages } from '@mango/file/admin-pages';
export { registerMangoJobAdminPages } from '@mango/job/admin-pages';
export { registerMangoNoticeAdminPages } from '@mango/notice/admin-pages';
export { registerMangoNoticeAdminShell } from '@mango/notice/admin-shell';
export { registerMangoNumgenAdminPages } from '@mango/numgen/admin-pages';
export { registerMangoTemplateAdminPages } from '@mango/template/admin-pages';
export { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';
export { registerMangoWorkflowBusinessExampleAdminPages } from '@mango/workflow-business-example/admin-pages';

import type { MangoAdminFeatureRegistrar } from '@mango/admin-shell';
import { registerMangoCalendarAdminPages } from '@mango/calendar/admin-pages';
import { registerMangoFileAdminPages } from '@mango/file/admin-pages';
import { registerMangoJobAdminPages } from '@mango/job/admin-pages';
import { registerMangoNoticeAdminPages } from '@mango/notice/admin-pages';
import { registerMangoNoticeAdminShell } from '@mango/notice/admin-shell';
import { registerMangoNumgenAdminPages } from '@mango/numgen/admin-pages';
import { registerMangoTemplateAdminPages } from '@mango/template/admin-pages';
import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';
import { registerMangoWorkflowBusinessExampleAdminPages } from '@mango/workflow-business-example/admin-pages';

export const mangoFullAdminFeatureRegistrars: MangoAdminFeatureRegistrar[] = [
  registerMangoFileAdminPages,
  registerMangoJobAdminPages,
  registerMangoTemplateAdminPages,
  registerMangoNoticeAdminPages,
  registerMangoNoticeAdminShell,
  registerMangoNumgenAdminPages,
  registerMangoCalendarAdminPages,
  registerMangoWorkflowAdminPages,
  registerMangoWorkflowBusinessExampleAdminPages,
];
