import { inject } from 'vue';
import type { HttpClient } from '@mango/api-schema';
import { MANGO_HTTP_CLIENT_KEY } from '@mango/app-runtime';
import { create{{aggregatePascal}}Api } from '@{{projectKebab}}/{{moduleKebab}}-api';
import type { {{aggregatePascal}}Api } from '@{{projectKebab}}/{{moduleKebab}}-api';

const apiByClient = new WeakMap<HttpClient, {{aggregatePascal}}Api>();

export function use{{aggregatePascal}}Api(): {{aggregatePascal}}Api {
  const client = inject<HttpClient | undefined>(MANGO_HTTP_CLIENT_KEY, undefined);
  if (!client) {
    throw new Error('{{modulePascal}} pages require an HttpClient provided by the current Vue app');
  }
  let api = apiByClient.get(client);
  if (!api) {
    api = create{{aggregatePascal}}Api(client);
    apiByClient.set(client, api);
  }
  return api;
}
