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
      'cms/sites/index': () => import('./views/SitesView.vue').then((module) => module.default),
      'cms/site-categories/index': () => import('./views/SiteCategoriesView.vue').then((module) => module.default),
      'cms/contents/index': () => import('./views/ContentsView.vue').then((module) => module.default),
      'cms/content-categories/index': () =>
        import('./views/ContentCategoriesView.vue').then((module) => module.default),
      'cms/content-tags/index': () => import('./views/ContentTagsView.vue').then((module) => module.default),
      'cms/content-publishes/index': () => import('./views/ContentPublishesView.vue').then((module) => module.default),
      'cms/navigations/index': () => import('./views/NavigationsView.vue').then((module) => module.default),
      'cms/advertisements/index': () => import('./views/AdvertisementsView.vue').then((module) => module.default),
      'cms/ad-deliveries/index': () => import('./views/AdDeliveriesView.vue').then((module) => module.default),
    },
  });
}
