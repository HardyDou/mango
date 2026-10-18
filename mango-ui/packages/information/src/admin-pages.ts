import { registerModulePages } from '@mango/admin-extension/core';

let registered = false;

export function registerMangoInformationAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-information',
    pages: {
      'information/forms/index': () => import('./views/FormsView.vue').then((module) => module.default),
      'information/forms/detail': () => import('./views/FormDetailView.vue').then((module) => module.default),
      'information/forms/config': () => import('./views/FormConfigView.vue').then((module) => module.default),
      'information/forms/submissions': () => import('./views/FormSubmissionsView.vue').then((module) => module.default),
      'information/forms/hosted': () => import('./views/FormHostedView.vue').then((module) => module.default),
      'information/hosted/index': () => import('./views/HostedFormView.vue').then((module) => module.default),
    },
    routes: [
      {
        path: '/information/forms/:id/detail',
        component: 'information/forms/detail',
        menuName: '表单详情',
        menuCode: 'information:form-detail',
        visible: 0,
      },
      {
        path: '/information/forms/:id/config',
        component: 'information/forms/config',
        menuName: '表单配置',
        menuCode: 'information:form-config',
        visible: 0,
      },
      {
        path: '/information/forms/:id/hosted',
        component: 'information/forms/hosted',
        menuName: '接入方式',
        menuCode: 'information:form-hosted',
        visible: 0,
      },
      {
        path: '/information/forms/:id/submissions',
        component: 'information/forms/submissions',
        menuName: '提交记录',
        menuCode: 'information:form-submissions',
        visible: 0,
      },
      {
        path: '/information/hosted',
        component: 'information/hosted/index',
        menuName: '托管表单',
        menuCode: 'information:hosted',
        visible: 0,
      },
    ],
  });
}
