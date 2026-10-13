import { inject } from 'vue';
import type { HttpClient } from '@mango/api-schema';
const MANGO_HTTP_CLIENT_KEY = 'mangoHttpClient';
import { createFileApi, fileApi, type FileApi } from '../api/file';
import { createFileDirectoryApi, type FileDirectoryApi } from '../api/fileDirectory';
import { createFileStorageApi, fileStorageApi, type FileStorageApi } from '../api/fileStorage';
import { createFileSettingsApi, fileSettingsApi, type FileSettingsApi } from '../api/fileSettings';

const apiByClient = new WeakMap<HttpClient, FileApi>();
const settingsApiByClient = new WeakMap<HttpClient, FileSettingsApi>();
const directoryApiByClient = new WeakMap<HttpClient, FileDirectoryApi>();
const storageApiByClient = new WeakMap<HttpClient, FileStorageApi>();

export function useFileApi(): FileApi {
  const client = inject<HttpClient | undefined>(MANGO_HTTP_CLIENT_KEY, undefined);
  if (!client) return fileApi;
  let api = apiByClient.get(client);
  if (!api) {
    api = createFileApi(client);
    apiByClient.set(client, api);
  }
  return api;
}

export function useFileDirectoryApi(): FileDirectoryApi {
  const client = inject<HttpClient | undefined>(MANGO_HTTP_CLIENT_KEY, undefined);
  if (!client) return createFileDirectoryApi();
  let api = directoryApiByClient.get(client);
  if (!api) {
    api = createFileDirectoryApi(client);
    directoryApiByClient.set(client, api);
  }
  return api;
}

export function useFileStorageApi(): FileStorageApi {
  const client = inject<HttpClient | undefined>(MANGO_HTTP_CLIENT_KEY, undefined);
  if (!client) return fileStorageApi;
  let api = storageApiByClient.get(client);
  if (!api) {
    api = createFileStorageApi(client);
    storageApiByClient.set(client, api);
  }
  return api;
}

export function useFileSettingsApi(): FileSettingsApi {
  const client = inject<HttpClient | undefined>(MANGO_HTTP_CLIENT_KEY, undefined);
  if (!client) return fileSettingsApi;
  let api = settingsApiByClient.get(client);
  if (!api) {
    api = createFileSettingsApi(client);
    settingsApiByClient.set(client, api);
  }
  return api;
}
