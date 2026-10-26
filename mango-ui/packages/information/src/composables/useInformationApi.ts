import { inject } from 'vue';
import type { HttpClient } from '@mango/api-schema';
import { MANGO_HTTP_CLIENT_KEY } from '@mango/app-runtime';
import { createInformationApi, type InformationApi } from '../api/information';

const apiByClient = new WeakMap<HttpClient, InformationApi>();

export function useInformationApi() {
  const client = inject<HttpClient | undefined>(MANGO_HTTP_CLIENT_KEY, undefined);
  if (!client) throw new Error('@mango/information requires an injected Mango HttpClient');
  let api = apiByClient.get(client);
  if (!api) {
    api = createInformationApi(client);
    apiByClient.set(client, api);
  }
  return api;
}
