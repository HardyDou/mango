import { registerModulePages } from '@mango/admin-pages/core';

let registered = false;

export function registerMangoTemplateAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-template',
    pages: {
      'system/template/index': () => import('./index').then(m => m.TemplateListView),
      'template/templates/index': () => import('./index').then(m => m.TemplateListView),
      'template/categories/index': () => import('./index').then(m => m.TemplateCategoryView),
      'template/render-records/index': () => import('./index').then(m => m.TemplateRenderRecordsView),
      'debug/capabilities/template': () => import('./index').then(m => m.TemplateServiceGuideView),
    },
  });
}
