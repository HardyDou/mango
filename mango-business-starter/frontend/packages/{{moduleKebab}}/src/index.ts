import { registerModulePages } from '@mango/admin-pages/core';
import type { HttpClient } from '@mango/api-schema';
import { configure{{aggregatePascal}}Api } from './api-context';

export const {{moduleCamel}}PageRegistry = {
  moduleCode: '{{moduleKebab}}',
  pages: {
    '{{moduleKebab}}/{{aggregateKebab}}/index': () => import('./views/{{moduleKebab}}/{{aggregateKebab}}/index.vue'),
  },
};

export function register{{modulePascal}}Pages(client: HttpClient) {
  configure{{aggregatePascal}}Api(client);
  registerModulePages({{moduleCamel}}PageRegistry);
}

export * from '@{{projectKebab}}/{{moduleKebab}}-api';
