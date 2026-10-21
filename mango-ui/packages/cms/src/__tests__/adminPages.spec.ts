import { describe, expect, it } from 'vitest';
import { getPageLoader } from '@mango/admin-pages/core';
import { registerMangoCmsAdminPages } from '../admin-pages';

const cmsPages = [
  ['cms/sites/index', 'SitesView.vue'],
  ['cms/site-categories/index', 'SiteCategoriesView.vue'],
  ['cms/contents/index', 'ContentsView.vue'],
  ['cms/content-categories/index', 'ContentCategoriesView.vue'],
  ['cms/content-tags/index', 'ContentTagsView.vue'],
  ['cms/content-publishes/index', 'ContentPublishesView.vue'],
  ['cms/navigations/index', 'NavigationsView.vue'],
  ['cms/advertisements/index', 'AdvertisementsView.vue'],
  ['cms/ad-deliveries/index', 'AdDeliveriesView.vue'],
] as const;

describe('CMS admin page registration', () => {
  it('registers every content operation page with a direct view loader', () => {
    registerMangoCmsAdminPages();

    for (const [component, viewFile] of cmsPages) {
      const loader = getPageLoader('mango-cms', component);
      expect(loader, component).toBeTypeOf('function');
      expect(String(loader), component).toContain(viewFile);
      expect(String(loader), component).toContain('.then');
      expect(String(loader), component).toContain('module.default');
    }
  });
});
