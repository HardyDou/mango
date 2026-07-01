import { registerModulePages } from '@mango/admin-pages/core';

let registered = false;

export function registerMangoLinkAdminPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerModulePages({
    moduleCode: 'mango-link',
    pages: {
      'link/company/index': () => import('./index').then(m => m.LinkCompanyLinksView),
      'link/favorites/index': () => import('./index').then(m => m.LinkFavoritesView),
      'link/my-links/index': () => import('./index').then(m => m.LinkMyLinksView),
      'link/categories/index': () => import('./index').then(m => m.LinkCategoriesView),
      'link/items/index': () => import('./index').then(m => m.LinkItemsView),
    },
  });
}
