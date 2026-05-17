import { registerModulePages, type MangoPageRegistry } from './core';

let registered = false;

export function registerDefaultAdminPages() {
  if (registered) {
    return;
  }
  registered = true;

  const registries: MangoPageRegistry[] = [
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
        'system/file/index': () => import('@mango/system/src/views/file/index.vue'),
        'system/file-storage/index': () => import('@mango/system/src/views/file-storage/index.vue'),
      },
    },
    {
      moduleCode: 'mango-workflow',
      pages: {
        'system/workflow-definition/index': () => import('@mango/workflow/src/views/workflow-definition/index.vue'),
        'workflow/task/todo/index': () => import('@mango/workflow/src/views/task-list/index.vue'),
        'workflow/task/initiated/index': () => import('@mango/workflow/src/views/task-list/index.vue'),
        'workflow/task/done/index': () => import('@mango/workflow/src/views/task-list/index.vue'),
        'workflow/task/copied/index': () => import('@mango/workflow/src/views/task-list/index.vue'),
        'workflow/task-list/index': () => import('@mango/workflow/src/views/task-list/index.vue'),
        'workflow/task/detail/index': () => import('@mango/workflow/src/views/task-detail/index.vue'),
        'workflow/start-process/index': () => import('@mango/workflow/src/views/start-process/index.vue'),
        'workflow/business-form/index': () => import('@mango/workflow/src/views/business-form/index.vue'),
      },
    },
  ];

  registries.forEach(registerModulePages);
}
