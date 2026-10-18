import { registerModulePages } from '@mango/admin-extension/core';

let registered = false;
export function registerMangoInformationAdminPages() {
  if (registered) return;
  registered = true;
  registerModulePages({
    moduleCode: 'mango-information',
    pages: {
      'information/forms/index': () => import('./views/FormsView.vue').then((module) => module.default),
      'information/submissions/index': () => import('./views/SubmissionsView.vue').then((module) => module.default),
      'information/hosted/index': () => import('./views/HostedFormView.vue').then((module) => module.default),
    },
    routes: [
      {
        path: '/information/hosted/:formCode',
        component: 'information/hosted/index',
        menuName: '托管表单',
        menuCode: 'information:hosted',
        visible: 0,
      },
    ],
  });
}
