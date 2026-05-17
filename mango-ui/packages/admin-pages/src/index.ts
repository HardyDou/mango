export type MangoPageLoader = () => Promise<unknown>;

export type MangoPageRegistry = {
  moduleCode: string;
  pages: Record<string, MangoPageLoader>;
};

const pageLoaders = new Map<string, MangoPageLoader>();
const moduleByPage = new Map<string, string>();

export function normalizeComponentPath(componentPath?: string) {
  return (componentPath || '')
    .replace(/^@\//, '')
    .replace(/^\//, '')
    .replace(/^src\//, '')
    .replace(/^views\//, '')
    .replace(/\.vue$/, '');
}

export function registerModulePages(registry: MangoPageRegistry) {
  Object.entries(registry.pages).forEach(([component, loader]) => {
    const normalized = normalizeComponentPath(component);
    pageLoaders.set(`${registry.moduleCode}:${normalized}`, loader);
    moduleByPage.set(normalized, registry.moduleCode);
  });
}

export function registerPage(moduleCode: string, component: string, loader: MangoPageLoader) {
  const normalized = normalizeComponentPath(component);
  pageLoaders.set(`${moduleCode}:${normalized}`, loader);
  moduleByPage.set(normalized, moduleCode);
}

export function getPageLoader(moduleCode?: string, component?: string) {
  const normalized = normalizeComponentPath(component);
  if (moduleCode) {
    return pageLoaders.get(`${moduleCode}:${normalized}`);
  }
  for (const [key, loader] of pageLoaders.entries()) {
    if (key.endsWith(`:${normalized}`)) {
      return loader;
    }
  }
  return undefined;
}

export function resolvePageModuleCode(component?: string, path?: string) {
  const normalizedComponent = normalizeComponentPath(component);
  const byComponent = moduleByPage.get(normalizedComponent);
  if (byComponent) {
    return byComponent;
  }
  const normalizedRoute = normalizeRoutePath(path);
  const routeAsIndex = normalizedRoute ? `${normalizedRoute}/index` : '';
  return moduleByPage.get(routeAsIndex) || moduleByPage.get(normalizedRoute);
}

function normalizeRoutePath(path?: string) {
  return (path || '')
    .replace(/^#/, '')
    .replace(/^\//, '')
    .replace(/\/$/, '');
}

export function registerDefaultAdminPages() {
  const registries: MangoPageRegistry[] = [
    {
      moduleCode: 'mango-authorization',
      pages: {
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
        'system/file/index': () => import('@mango/system').then(m => m.FileView),
        'system/file-storage/index': () => import('@mango/system').then(m => m.FileStorageView),
      },
    },
    {
      moduleCode: 'mango-workflow',
      pages: {
        'system/workflow-definition/index': () => import('@mango/workflow').then(m => m.WorkflowDefinitionView),
        'workflow/task/todo/index': () => import('@mango/workflow').then(m => m.WorkflowTaskListView),
        'workflow/task/initiated/index': () => import('@mango/workflow').then(m => m.WorkflowTaskListView),
        'workflow/task/done/index': () => import('@mango/workflow').then(m => m.WorkflowTaskListView),
        'workflow/task/copied/index': () => import('@mango/workflow').then(m => m.WorkflowTaskListView),
        'workflow/task-list/index': () => import('@mango/workflow').then(m => m.WorkflowTaskListView),
        'workflow/task/detail/index': () => import('@mango/workflow').then(m => m.WorkflowTaskDetailView),
        'workflow/start-process/index': () => import('@mango/workflow').then(m => m.WorkflowStartProcessView),
        'workflow/business-form/index': () => import('@mango/workflow').then(m => m.WorkflowBusinessFormView),
      },
    },
  ];

  registries.forEach(registerModulePages);
}
