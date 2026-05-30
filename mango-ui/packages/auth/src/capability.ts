const authPages = {
  'login/index': () => import('./views/login.vue').then(m => m.default),
  'profile/index': () => import('./views/profile.vue').then(m => m.default),
  'password/index': () => import('./views/password.vue').then(m => m.default),
};

export const mangoAuthPageRegistry = {
  moduleCode: 'mango-authorization',
  pages: authPages,
};

export const mangoAuthCapability = {
  moduleCode: 'mango-authorization',
  packageName: '@mango/auth',
  capabilityCode: 'auth',
  capabilityName: '认证与个人中心',
  requires: [],
  optional: [],
  backend: {
    moduleCode: 'mango-authorization',
    menuSource: 'backend',
    resourceManifest: 'META-INF/mango/resource-manifest.json',
    requiredApis: ['/api/auth/login', '/api/auth/login-institutions', '/api/auth/logout'],
  },
  pages: [
    { component: 'login/index', loader: authPages['login/index'] },
    { component: 'profile/index', loader: authPages['profile/index'] },
    { component: 'password/index', loader: authPages['password/index'] },
  ],
  menus: [],
  permissions: [],
  styles: [],
  runtime: {
    modes: ['local', 'micro', 'mixed'],
    defaultMode: 'local',
  },
  e2e: {
    smoke: ['login', 'profile', 'password'],
    screenshots: ['login', 'profile'],
    dataChecks: ['login-institutions', 'current-user'],
  },
};
