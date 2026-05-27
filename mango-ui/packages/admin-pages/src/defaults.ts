import { registerModulePages, registerShellPages, type MangoPageRegistry, type MangoShellPageLoaders } from './core';
import { registerWorkflowBusinessExampleComponents } from '@mango/workflow-business-example';

let registered = false;

export type RegisterDefaultAdminPagesOptions = {
  shellPages?: MangoShellPageLoaders;
  registries?: MangoPageRegistry[];
};

export function registerDefaultAdminPages(options: RegisterDefaultAdminPagesOptions = {}) {
  if (registered) {
    return;
  }
  registered = true;
  registerWorkflowBusinessExampleComponents();
  registerShellPages(options.shellPages || {});

  const registries: MangoPageRegistry[] = [
    {
      moduleCode: 'mango-authorization',
      pages: {
        'profile/index': () => import('@mango/auth').then(m => m.ProfileView),
        'password/index': () => import('@mango/auth').then(m => m.PasswordView),
        'system/menu-package/index': () => import('@mango/rbac').then(m => m.MenuPackageView),
        'system/menu/index': () => import('@mango/rbac').then(m => m.MenuView),
        'system/role/index': () => import('@mango/rbac').then(m => m.RoleView),
        'system/user/index': () => import('@mango/rbac').then(m => m.UserView),
        'system/org/index': () => import('@mango/rbac').then(m => m.OrgView),
        'system/post/index': () => import('@mango/rbac').then(m => m.PostView),
        'system/app/index': () => import('@mango/rbac').then(m => m.AppView),
        'system/permission/index': () => import('@mango/rbac').then(m => m.PermissionView),
      },
    },
    {
      moduleCode: 'mango-system',
      pages: {
        'system/dict/index': () => import('@mango/system').then(m => m.DictView),
        'system/operation-log/index': () => import('@mango/system').then(m => m.OperationLogView),
        'system/login-log/index': () => import('@mango/system').then(m => m.LoginLogView),
        'system/tenant/index': () => import('@mango/system').then(m => m.TenantView),
        'system/config/index': () => import('@mango/system').then(m => m.ConfigView),
        'system/route/index': () => import('@mango/system').then(m => m.RouteView),
        'system/public-path/index': () => import('@mango/system').then(m => m.PublicPathView),
        'system/area/index': () => import('@mango/system').then(m => m.AreaView),
      },
    },
    {
      moduleCode: 'mango-template',
      pages: {
        'system/template/index': () => import('@mango/template').then(m => m.TemplateListView),
        'template/templates/index': () => import('@mango/template').then(m => m.TemplateListView),
        'template/categories/index': () => import('@mango/template').then(m => m.TemplateCategoryView),
        'template/render-records/index': () => import('@mango/template').then(m => m.TemplateRenderRecordsView),
        'debug/capabilities/template': () => import('@mango/template').then(m => m.TemplateServiceGuideView),
      },
    },
    {
      moduleCode: 'mango-file',
      pages: {
        'file/files/index': () => import('@mango/file').then(m => m.FileView),
        'file/storage-configs/index': () => import('@mango/file').then(m => m.FileStorageView),
        'file/settings/index': () => import('@mango/file').then(m => m.FileSettingsView),
      },
    },
    {
      moduleCode: 'mango-numgen',
      pages: {
        'platform/numgen/index': () => import('@mango/numgen').then(m => m.NumgenView),
        'numgen/index': () => import('@mango/numgen').then(m => m.NumgenView),
      },
    },
    {
      moduleCode: 'mango-calendar',
      pages: {
        'data/calendar/index': () => import('@mango/calendar').then(m => m.CalendarView),
      },
    },
    {
      moduleCode: 'mango-workflow',
      pages: {
        'workflow/definition/index': () => import('@mango/workflow').then(m => m.WorkflowDefinitionView),
        'system/workflow-definition/index': () => import('@mango/workflow').then(m => m.WorkflowDefinitionView),
        'workflow/template/index': () => import('@mango/workflow').then(m => m.WorkflowTemplateView),
        'workflow-template/index': () => import('@mango/workflow').then(m => m.WorkflowTemplateView),
        'workflow/task/todo/index': () => import('@mango/workflow').then(m => m.WorkflowTaskListView),
        'workflow/task/initiated/index': () => import('@mango/workflow').then(m => m.WorkflowTaskListView),
        'workflow/task/done/index': () => import('@mango/workflow').then(m => m.WorkflowTaskListView),
        'workflow/task/copied/index': () => import('@mango/workflow').then(m => m.WorkflowTaskListView),
        'workflow/task-list/index': () => import('@mango/workflow').then(m => m.WorkflowTaskListView),
        'workflow/task/detail/index': () => import('@mango/workflow').then(m => m.WorkflowTaskDetailView),
        'workflow/start-process/index': () => import('@mango/workflow').then(m => m.WorkflowStartProcessView),
        'workflow/custom-apply/index': () => import('@mango/workflow').then(m => m.WorkflowCustomApplyView),
        'workflow/business-form/index': () => import('@mango/workflow-business-example').then(m => m.WorkflowBusinessFormView),
      },
    },
  ];

  registries.forEach(registerModulePages);
  (options.registries || []).forEach(registerModulePages);
}
