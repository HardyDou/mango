import { registerModulePages } from '@mango/admin-pages/core';
import { systemLinkNavigationWidgets } from '@mango/grid-widgets';

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

  return {
    businessDomainCode: 'mango-link',
    businessDomainName: '链接',
    groupName: '工作台',
    widgets: systemLinkNavigationWidgets,
  };
}
