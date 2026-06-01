import { getMangoDevComponentPages } from '@mango/admin-pages/dev-pages';
import { resolveMangoAdminFeatures } from '@mango/admin-pages/features';
import { registerModulePages } from '@mango/admin-pages/core';
import type { MangoPageLoader } from '@mango/admin-pages/core';
import { getMangoAdminShellOptions } from '../../config';

export function registerMangoAdminShellDevPages() {
  const enabledFeatures = resolveMangoAdminFeatures(getMangoAdminShellOptions().features);
  registerModulePages({
    moduleCode: 'mango-shell',
    pages: getMangoDevComponentPages()
      .filter(page => !page.feature || enabledFeatures.has(page.feature))
      .reduce<Record<string, MangoPageLoader>>((pages, page) => {
        pages[page.component] = page.loader;
        return pages;
      }, {}),
  });
}
