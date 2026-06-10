import { registerMangoCalendarAdminPages } from '@mango/calendar/admin-pages';
import { registerMangoFileAdminPages } from '@mango/file/admin-pages';
import { registerMangoNoticeAdminPages } from '@mango/notice/admin-pages';
import { registerMangoNoticeAdminShell } from '@mango/notice/admin-shell';
import { registerMangoNumgenAdminPages } from '@mango/numgen/admin-pages';
import { registerMangoPaymentAdminPages } from '@mango/payment/admin-pages';
import { registerMangoTemplateAdminPages } from '@mango/template/admin-pages';
import { registerMangoWorkflowAdminPages } from '@mango/workflow/admin-pages';
import { registerMangoWorkflowBusinessExampleAdminPages } from '@mango/workflow-business-example/admin-pages';

export function registerFullMangoAdminFeaturePages() {
  registerMangoFileAdminPages();
  registerMangoTemplateAdminPages();
  registerMangoNoticeAdminPages();
  registerMangoNoticeAdminShell();
  registerMangoNumgenAdminPages();
  registerMangoCalendarAdminPages();
  registerMangoPaymentAdminPages();
  registerMangoWorkflowAdminPages();
  registerMangoWorkflowBusinessExampleAdminPages();
}
