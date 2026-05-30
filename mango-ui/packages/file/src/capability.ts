const filePages = {
  'file/files/index': () => import('./views/files/index.vue').then(m => m.default),
  'file/storage-configs/index': () => import('./views/storage-configs/index.vue').then(m => m.default),
  'file/settings/index': () => import('./views/settings/index.vue').then(m => m.default),
};

export const mangoFilePageRegistry = {
  moduleCode: 'mango-file',
  pages: filePages,
};

export const mangoFileCapability = {
  moduleCode: 'mango-file',
  packageName: '@mango/file',
  capabilityCode: 'file',
  capabilityName: '文件中心',
  requires: ['auth', 'rbac'],
  optional: [],
  backend: {
    moduleCode: 'mango-file',
    menuSource: 'backend',
    resourceManifest: 'META-INF/mango/resource-manifest.json',
    requiredApis: ['/api/file/files/page', '/api/file/storage-configs/page', '/api/file/settings'],
  },
  pages: [
    {
      component: 'file/files/index',
      loader: filePages['file/files/index'],
      menuCode: 'file:files',
      permissions: ['file:files:list'],
    },
    {
      component: 'file/storage-configs/index',
      loader: filePages['file/storage-configs/index'],
      menuCode: 'file:storage-configs',
      permissions: ['file:storage-configs:list'],
    },
    {
      component: 'file/settings/index',
      loader: filePages['file/settings/index'],
      menuCode: 'file:settings',
      permissions: ['file:settings:query'],
    },
  ],
  menus: [
    {
      menuCode: 'file:files',
      moduleCode: 'mango-file',
      component: 'file/files/index',
      permissions: ['file:files:list'],
      source: 'backend',
    },
    {
      menuCode: 'file:storage-configs',
      moduleCode: 'mango-file',
      component: 'file/storage-configs/index',
      permissions: ['file:storage-configs:list'],
      source: 'backend',
    },
    {
      menuCode: 'file:settings',
      moduleCode: 'mango-file',
      component: 'file/settings/index',
      permissions: ['file:settings:query'],
      source: 'backend',
    },
  ],
  permissions: ['file:files:list', 'file:storage-configs:list', 'file:settings:query'],
  styles: [],
  runtime: {
    modes: ['local', 'micro', 'mixed'],
    defaultMode: 'local',
  },
  e2e: {
    smoke: ['file-management'],
    screenshots: ['file-files', 'file-storage-configs', 'file-settings'],
    dataChecks: ['file-page', 'storage-config-page'],
  },
};
