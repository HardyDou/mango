import { registerModulePages } from '@mango/admin-pages/core';

export const {{moduleCamel}}PageRegistry = {
  moduleCode: '{{moduleKebab}}',
  pages: {
    '{{moduleKebab}}/{{aggregateKebab}}/index': () => import('./views/{{moduleKebab}}/{{aggregateKebab}}/index.vue'),
  },
};

export function register{{modulePascal}}Pages() {
  registerModulePages({{moduleCamel}}PageRegistry);
}

export * from '@{{projectKebab}}/{{moduleKebab}}-api';
