import { registerModulePages } from '@mango/admin-pages/core';

export const {{moduleCamel}}PageRegistry = {
  moduleCode: '{{moduleKebab}}',
  pages: {
    '{{moduleKebab}}/{{aggregateKebab}}/index': () => import('./views/{{moduleKebab}}/{{aggregateKebab}}/index.vue'),
  },
};

let registered = false;

export function register{{modulePascal}}Pages() {
  if (!registered) {
    registered = true;
    registerModulePages({{moduleCamel}}PageRegistry);
  }

  return {
    businessDomainCode: '{{moduleBusinessDomainCode}}',
    businessDomainName: '{{moduleName}}',
    groupName: '{{moduleName}}',
    widgets: [],
  };
}

export * from '@{{projectKebab}}/{{moduleKebab}}-api';
