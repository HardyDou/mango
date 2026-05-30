const templatePages = {
  'system/template/index': () => import('./views/templates/index.vue').then(m => m.default),
  'template/templates/index': () => import('./views/templates/index.vue').then(m => m.default),
  'template/categories/index': () => import('./views/categories/index.vue').then(m => m.default),
  'template/render-records/index': () => import('./views/render-records/index.vue').then(m => m.default),
  'debug/capabilities/template': () => import('./views/platform-capabilities/template/index.vue').then(m => m.default),
};

export const mangoTemplatePageRegistry = {
  moduleCode: 'mango-template',
  pages: templatePages,
};

export const mangoTemplateCapability = {
  moduleCode: 'mango-template',
  packageName: '@mango/template',
  capabilityCode: 'template',
  capabilityName: '模板中心',
  requires: ['auth', 'rbac'],
  optional: ['file'],
  backend: {
    moduleCode: 'mango-template',
    menuSource: 'backend',
    resourceManifest: 'META-INF/mango/resource-manifest.json',
    requiredApis: ['/api/template/templates/page', '/api/template/categories/page', '/api/template/render-records/page'],
  },
  pages: [
    {
      component: 'system/template/index',
      loader: templatePages['system/template/index'],
      menuCode: 'template:template',
      permissions: ['template:template:list'],
    },
    {
      component: 'template/templates/index',
      loader: templatePages['template/templates/index'],
      menuCode: 'template:template',
      permissions: ['template:template:list'],
    },
    {
      component: 'template/categories/index',
      loader: templatePages['template/categories/index'],
      menuCode: 'template:category',
      permissions: ['template:category:list'],
    },
    {
      component: 'template/render-records/index',
      loader: templatePages['template/render-records/index'],
      menuCode: 'template:render-record',
      permissions: ['template:render-record:list'],
    },
    { component: 'debug/capabilities/template', loader: templatePages['debug/capabilities/template'] },
  ],
  menus: [
    {
      menuCode: 'template:template',
      moduleCode: 'mango-template',
      component: 'template/templates/index',
      permissions: ['template:template:list'],
      source: 'backend',
    },
    {
      menuCode: 'template:category',
      moduleCode: 'mango-template',
      component: 'template/categories/index',
      permissions: ['template:category:list'],
      source: 'backend',
    },
    {
      menuCode: 'template:render-record',
      moduleCode: 'mango-template',
      component: 'template/render-records/index',
      permissions: ['template:render-record:list'],
      source: 'backend',
    },
  ],
  permissions: ['template:template:list', 'template:category:list', 'template:render-record:list'],
  styles: [],
  runtime: {
    modes: ['local', 'micro', 'mixed'],
    defaultMode: 'local',
  },
  e2e: {
    smoke: ['template-management', 'template-document-syntax'],
    screenshots: ['template-templates', 'template-categories', 'template-render-records'],
    dataChecks: ['template-page', 'template-category-page', 'render-record-page'],
  },
};
