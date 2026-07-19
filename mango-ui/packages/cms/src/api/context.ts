import { inject } from 'vue';
import type { HttpClient } from '@mango/api-schema';
import { MANGO_HTTP_CLIENT_KEY } from '@mango/app-runtime';
import { createCmsApi, type CmsApi } from './cms';

const apiByClient = new WeakMap<HttpClient, CmsApi>();

export function useCmsApi() {
  const httpClient = inject<HttpClient | undefined>(MANGO_HTTP_CLIENT_KEY, undefined);
  if (!httpClient) {
    throw new Error('@mango/cms requires an injected Mango HttpClient');
  }
  let api = apiByClient.get(httpClient);
  if (!api) {
    api = createCmsApi(httpClient);
    apiByClient.set(httpClient, api);
  }
  return api;
}
