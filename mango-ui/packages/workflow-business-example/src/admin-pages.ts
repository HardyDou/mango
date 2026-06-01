import { registerModulePages } from '@mango/admin-pages/core';
import { registerWorkflowBusinessExampleComponents } from './register';

let registered = false;

export function registerMangoWorkflowBusinessExampleAdminPages() {
  registerWorkflowBusinessExampleComponents();
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-workflow',
    pages: {
      'workflow/business-form/index': () => import('./index').then(m => m.WorkflowBusinessFormView),
    },
  });
}
