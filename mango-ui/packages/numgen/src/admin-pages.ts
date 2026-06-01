import { registerModulePages } from '@mango/admin-pages/core';

let registered = false;

export function registerMangoNumgenAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-numgen',
    pages: {
      'platform/numgen/index': () => import('./index').then(m => m.NumgenView),
      'numgen/index': () => import('./index').then(m => m.NumgenView),
    },
  });
}
