import { registerModulePages } from '@mango/admin-pages/core';

let registered = false;

export function registerMangoCmsAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-cms',
    pages: {
      'cms/sites/index': () => import('./index').then(m => m.CmsSitesView),
      'cms/site-categories/index': () => import('./index').then(m => m.CmsSiteCategoriesView),
      'cms/contents/index': () => import('./index').then(m => m.CmsContentsView),
      'cms/content-categories/index': () => import('./index').then(m => m.CmsContentCategoriesView),
      'cms/content-tags/index': () => import('./index').then(m => m.CmsContentTagsView),
      'cms/content-publishes/index': () => import('./index').then(m => m.CmsContentPublishesView),
      'cms/navigations/index': () => import('./index').then(m => m.CmsNavigationsView),
      'cms/advertisements/index': () => import('./index').then(m => m.CmsAdvertisementsView),
      'cms/ad-deliveries/index': () => import('./index').then(m => m.CmsAdDeliveriesView),
    },
  });
}
