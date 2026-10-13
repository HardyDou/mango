import { inject } from 'vue';
import type { HttpClient } from '@mango/api-schema';
import * as legacyUpload from '../api/upload';
import { createUploadApi, type UploadApi } from '../api/upload';

const MANGO_HTTP_CLIENT_KEY = 'mangoHttpClient';

export function useUploadApi(): UploadApi {
  const client = inject<HttpClient | undefined>(MANGO_HTTP_CLIENT_KEY, undefined);
  if (!client) return legacyUpload as unknown as UploadApi;
  return createUploadApi(client);
}
