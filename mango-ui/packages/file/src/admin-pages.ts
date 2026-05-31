import { registerModulePages } from '@mango/admin-pages/core';

let registered = false;

export function registerMangoFileAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-file',
    pages: {
      'file/files/index': () => import('./index').then(m => m.FileView),
      'file/storage-configs/index': () => import('./index').then(m => m.FileStorageView),
      'file/settings/index': () => import('./index').then(m => m.FileSettingsView),
    },
  });
}
