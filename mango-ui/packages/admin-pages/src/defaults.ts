import { registerModulePages, type MangoPageLoader, type MangoPageRegistry } from './core';
import { DEV_COMPONENT_DEMO_PAGES } from './devComponentPages';
import { registerWorkflowBusinessExampleComponents } from '@mango/workflow-business-example';

let registered = false;

export function registerDefaultAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerWorkflowBusinessExampleComponents();

  const devComponentPageLoaders: Record<string, MangoPageLoader> = {
    'demo/components/EditorView': () => import('../../../apps/mango-admin/src/views/demo/components/EditorView.vue'),
    'demo/components/CodeEditorView': () => import('../../../apps/mango-admin/src/views/demo/components/CodeEditorView.vue'),
    'demo/components/UploadView': () => import('../../../apps/mango-admin/src/views/demo/components/UploadView.vue'),
    'demo/components/ChartsView': () => import('../../../apps/mango-admin/src/views/demo/components/ChartsView.vue'),
    'demo/components/DirectiveView': () => import('../../../apps/mango-admin/src/views/demo/components/DirectiveView.vue'),
    'demo/components/ChatView': () => import('../../../apps/mango-admin/src/views/demo/components/ChatView.vue'),
    'demo/components/RealtimeView': () => import('../../../apps/mango-admin/src/views/demo/components/RealtimeView.vue'),
    'demo/components/ChinaAreaView': () => import('../../../apps/mango-admin/src/views/demo/components/ChinaAreaView.vue'),
    'demo/components/OrgSelectorView': () => import('../../../apps/mango-admin/src/views/demo/components/OrgSelectorView.vue'),
    'demo/components/WorkflowComponentsView': () => import('../../../apps/mango-admin/src/views/demo/components/WorkflowComponentsView.vue'),
    'demo/components/CaptchaView': () => import('../../../apps/mango-admin/src/views/demo/components/CaptchaView.vue'),
  };
  const devComponentPages = import.meta.env.DEV ? DEV_COMPONENT_DEMO_PAGES.reduce<Record<string, MangoPageLoader>>((pages, page) => {
    pages[page.component] = devComponentPageLoaders[page.component];
    return pages;
  }, {}) : {};

  const registries: MangoPageRegistry[] = [
    {
      moduleCode: 'mango-shell',
      pages: {
        'home/index': () => import('../../../apps/mango-admin/src/views/home/index.vue'),
        'error/404': () => import('../../../apps/mango-admin-shell/src/views/error/404.vue'),
        'error/not-found': () => import('../../../apps/mango-admin-shell/src/views/error/404.vue'),
        ...devComponentPages,
      },
    },
    {
      moduleCode: 'mango-authorization',
      pages: {
        'profile/index': () => import('@mango/auth').then(m => m.ProfileView),
        'password/index': () => import('@mango/auth').then(m => m.PasswordView),
        'system/menu-package/index': () => import('@mango/rbac/src/views/menu-package/index.vue'),
        'system/menu/index': () => import('@mango/rbac/src/views/menu/index.vue'),
        'system/role/index': () => import('@mango/rbac/src/views/role/index.vue'),
        'system/user/index': () => import('@mango/rbac/src/views/user/index.vue'),
        'system/org/index': () => import('@mango/rbac/src/views/org/index.vue'),
        'system/post/index': () => import('@mango/rbac/src/views/post/index.vue'),
        'system/app/index': () => import('@mango/rbac/src/views/app/index.vue'),
        'system/permission/index': () => import('@mango/rbac/src/views/permission/index.vue'),
      },
    },
    {
      moduleCode: 'mango-system',
      pages: {
        'system/dict/index': () => import('@mango/system/src/views/dict/index.vue'),
        'system/operation-log/index': () => import('@mango/system/src/views/operation-log/index.vue'),
        'system/login-log/index': () => import('@mango/system/src/views/login-log/index.vue'),
        'system/tenant/index': () => import('@mango/system/src/views/tenant/index.vue'),
        'system/config/index': () => import('@mango/system/src/views/config/index.vue'),
        'system/route/index': () => import('@mango/system/src/views/route/index.vue'),
        'system/public-path/index': () => import('@mango/system/src/views/public-path/index.vue'),
        'system/area/index': () => import('@mango/system/src/views/area/index.vue'),
      },
    },
    {
      moduleCode: 'mango-template',
      pages: {
        'system/template/index': () => import('@mango/template/src/views/templates/index.vue'),
        'template/templates/index': () => import('@mango/template/src/views/templates/index.vue'),
        'template/categories/index': () => import('@mango/template/src/views/categories/index.vue'),
        'template/render-records/index': () => import('@mango/template/src/views/render-records/index.vue'),
        'debug/capabilities/template': () => import('@mango/template/src/views/platform-capabilities/template/index.vue'),
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
      moduleCode: 'mango-notice',
      pages: {
        'notice/business-config/index': () => import('@mango/notice').then(m => m.NoticeBusinessConfigView),
        'notice/message-definition/index': () => import('@mango/notice').then(m => m.NoticeMessageDefinitionView),
        'notice/send-message/index': () => import('@mango/notice').then(m => m.NoticeSendMessageView),
        'notice/channel/index': () => import('@mango/notice').then(m => m.NoticeChannelView),
        'notice/task/index': () => import('@mango/notice').then(m => m.NoticeTaskView),
        'notice/record/index': () => import('@mango/notice').then(m => m.NoticeRecordView),
        'notice/site-message/index': () => import('@mango/notice').then(m => m.NoticeSiteMessageView),
        'notice/site/messages/index': () => import('@mango/notice').then(m => m.NoticeSiteMessageView),
        'notice/setting/index': () => import('@mango/notice').then(m => m.NoticeSettingView),
        'notice/receive-setting/index': () => import('@mango/notice').then(m => m.NoticeReceiveSettingView),
        'notice/retry/index': () => import('@mango/notice').then(m => m.NoticeRetryView),
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
        'workflow/definition/index': () => import('@mango/workflow/src/views/workflow-definition/index.vue'),
        'system/workflow-definition/index': () => import('@mango/workflow/src/views/workflow-definition/index.vue'),
        'workflow/template/index': () => import('@mango/workflow/src/views/workflow-template/index.vue'),
        'workflow-template/index': () => import('@mango/workflow/src/views/workflow-template/index.vue'),
        'workflow/task/todo/index': () => import('@mango/workflow/src/views/task-list/index.vue'),
        'workflow/task/initiated/index': () => import('@mango/workflow/src/views/task-list/index.vue'),
        'workflow/task/done/index': () => import('@mango/workflow/src/views/task-list/index.vue'),
        'workflow/task/copied/index': () => import('@mango/workflow/src/views/task-list/index.vue'),
        'workflow/task-list/index': () => import('@mango/workflow/src/views/task-list/index.vue'),
        'workflow/task/detail/index': () => import('@mango/workflow/src/views/task-detail/index.vue'),
        'workflow/start-process/index': () => import('@mango/workflow/src/views/start-process/index.vue'),
        'workflow/custom-apply/index': () => import('@mango/workflow/src/views/custom-apply/index.vue'),
        'workflow/business-form/index': () => import('@mango/workflow-business-example/src/views/business-form/index.vue'),
      },
    },
  ];

  registries.forEach(registerModulePages);
}
