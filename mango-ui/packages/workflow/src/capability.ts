const workflowPages = {
  'workflow/definition/index': () => import('./views/workflow-definition/index.vue').then(m => m.default),
  'system/workflow-definition/index': () => import('./views/workflow-definition/index.vue').then(m => m.default),
  'workflow/template/index': () => import('./views/workflow-template/index.vue').then(m => m.default),
  'workflow-template/index': () => import('./views/workflow-template/index.vue').then(m => m.default),
  'workflow/task/todo/index': () => import('./views/task-list/index.vue').then(m => m.default),
  'workflow/task/initiated/index': () => import('./views/task-list/index.vue').then(m => m.default),
  'workflow/task/done/index': () => import('./views/task-list/index.vue').then(m => m.default),
  'workflow/task/copied/index': () => import('./views/task-list/index.vue').then(m => m.default),
  'workflow/task-list/index': () => import('./views/task-list/index.vue').then(m => m.default),
  'workflow/task/detail/index': () => import('./views/task-detail/index.vue').then(m => m.default),
  'workflow/start-process/index': () => import('./views/start-process/index.vue').then(m => m.default),
  'workflow/custom-apply/index': () => import('./views/custom-apply/index.vue').then(m => m.default),
};

export const mangoWorkflowPageRegistry = {
  moduleCode: 'mango-workflow',
  pages: workflowPages,
};

export const mangoWorkflowCapability = {
  moduleCode: 'mango-workflow',
  packageName: '@mango/workflow',
  capabilityCode: 'workflow',
  capabilityName: '工作流',
  requires: ['auth', 'rbac'],
  optional: ['template'],
  backend: {
    moduleCode: 'mango-workflow',
    menuSource: 'backend',
    resourceManifest: 'META-INF/mango/resource-manifest.json',
    requiredApis: ['/api/workflow/definitions/page', '/api/workflow/templates/page', '/api/workflow/tasks/page'],
  },
  pages: [
    {
      component: 'workflow/definition/index',
      loader: workflowPages['workflow/definition/index'],
      menuCode: 'workflow:definition',
      permissions: ['workflow:definition:list'],
    },
    {
      component: 'system/workflow-definition/index',
      loader: workflowPages['system/workflow-definition/index'],
      menuCode: 'workflow:definition',
      permissions: ['workflow:definition:list'],
    },
    {
      component: 'workflow/template/index',
      loader: workflowPages['workflow/template/index'],
      menuCode: 'workflow:template',
      permissions: ['workflow:template:list'],
    },
    {
      component: 'workflow-template/index',
      loader: workflowPages['workflow-template/index'],
      menuCode: 'workflow:template',
      permissions: ['workflow:template:list'],
    },
    { component: 'workflow/task/todo/index', loader: workflowPages['workflow/task/todo/index'] },
    { component: 'workflow/task/initiated/index', loader: workflowPages['workflow/task/initiated/index'] },
    { component: 'workflow/task/done/index', loader: workflowPages['workflow/task/done/index'] },
    { component: 'workflow/task/copied/index', loader: workflowPages['workflow/task/copied/index'] },
    { component: 'workflow/task-list/index', loader: workflowPages['workflow/task-list/index'] },
    { component: 'workflow/task/detail/index', loader: workflowPages['workflow/task/detail/index'] },
    { component: 'workflow/start-process/index', loader: workflowPages['workflow/start-process/index'] },
    { component: 'workflow/custom-apply/index', loader: workflowPages['workflow/custom-apply/index'] },
  ],
  menus: [
    {
      menuCode: 'workflow:definition',
      moduleCode: 'mango-workflow',
      component: 'workflow/definition/index',
      permissions: ['workflow:definition:list'],
      source: 'backend',
    },
    {
      menuCode: 'workflow:template',
      moduleCode: 'mango-workflow',
      component: 'workflow/template/index',
      permissions: ['workflow:template:list'],
      source: 'backend',
    },
  ],
  permissions: ['workflow:definition:list', 'workflow:template:list'],
  styles: [],
  runtime: {
    modes: ['local', 'micro', 'mixed'],
    defaultMode: 'local',
  },
  e2e: {
    smoke: ['workflow-management'],
    screenshots: ['workflow-definition', 'workflow-template', 'workflow-task-list'],
    dataChecks: ['workflow-definition-page', 'workflow-template-page', 'workflow-task-page'],
  },
};
