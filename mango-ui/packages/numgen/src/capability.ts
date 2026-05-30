const numgenPages = {
  'platform/numgen/index': () => import('./views/numgen/index.vue').then(m => m.default),
  'numgen/index': () => import('./views/numgen/index.vue').then(m => m.default),
};

export const mangoNumgenPageRegistry = {
  moduleCode: 'mango-numgen',
  pages: numgenPages,
};

export const mangoNumgenCapability = {
  moduleCode: 'mango-numgen',
  packageName: '@mango/numgen',
  capabilityCode: 'numgen',
  capabilityName: '编号规则',
  requires: ['auth', 'rbac'],
  optional: [],
  backend: {
    moduleCode: 'mango-numgen',
    menuSource: 'backend',
    resourceManifest: 'META-INF/mango/resource-manifest.json',
    requiredApis: ['/api/numgen/rules/page'],
  },
  pages: [
    {
      component: 'platform/numgen/index',
      loader: numgenPages['platform/numgen/index'],
      menuCode: 'data:numgen',
      permissions: ['numgen:manage:list'],
    },
    {
      component: 'numgen/index',
      loader: numgenPages['numgen/index'],
      menuCode: 'data:numgen',
      permissions: ['numgen:manage:list'],
    },
  ],
  menus: [
    {
      menuCode: 'data:numgen',
      moduleCode: 'mango-numgen',
      component: 'platform/numgen/index',
      permissions: ['numgen:manage:list'],
      source: 'backend',
    },
  ],
  permissions: ['numgen:manage:list'],
  styles: [],
  runtime: {
    modes: ['local', 'micro', 'mixed'],
    defaultMode: 'local',
  },
  e2e: {
    smoke: ['numgen-management'],
    screenshots: ['data-numgen'],
    dataChecks: ['numgen-rule-page'],
  },
};
