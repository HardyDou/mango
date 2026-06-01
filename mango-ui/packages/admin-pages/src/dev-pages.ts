import type { DevComponentDemoPage } from './devComponentPages';
import type { MangoAdminFeatureCode } from './features';
import type { MangoPageLoader } from './core';

export type MangoDevComponentPageRegistration = DevComponentDemoPage & {
  loader: MangoPageLoader;
  feature?: MangoAdminFeatureCode;
};

const devComponentPages: MangoDevComponentPageRegistration[] = [];

export function registerMangoDevComponentPages(pages: MangoDevComponentPageRegistration[]) {
  for (const page of pages) {
    if (!devComponentPages.some(registered => registered.component === page.component)) {
      devComponentPages.push(page);
    }
  }
}

export function getMangoDevComponentPages() {
  return [...devComponentPages].sort((left, right) => left.sort - right.sort);
}
