import { createAiModelManagementApi, type AiModelManagementApi } from '@mango/ai-api';
import type { HttpClient } from '@mango/api-schema';
import { MANGO_HTTP_CLIENT_KEY } from '@mango/app-runtime';
import { inject } from 'vue';

const apiByClient = new WeakMap<HttpClient, AiModelManagementApi>();

export function useAiConfigurationApi() {
  const client = inject<HttpClient | undefined>(MANGO_HTTP_CLIENT_KEY, undefined);
  if (!client) throw new Error('@mango/ai requires an injected Mango HttpClient');
  let api = apiByClient.get(client);
  if (!api) {
    api = createAiModelManagementApi(client);
    apiByClient.set(client, api);
  }
  return api;
}
