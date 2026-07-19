import type { HttpClient, HttpRequest } from '@mango/api-schema';
import { del, get, post, put } from '@mango/common/utils/request';
import { createCmsApi } from './cms';

const legacyHttpClient: HttpClient = {
  request<TResponse, TBody>(request: HttpRequest<TBody>) {
    const config = {
      params: request.query,
      signal: request.signal,
    };
    if (request.method === 'GET') return get<TResponse>(request.url, config);
    if (request.method === 'POST') return post<TResponse>(request.url, request.body, config);
    if (request.method === 'PUT') return put<TResponse>(request.url, request.body, config);
    if (request.method === 'DELETE') return del<TResponse>(request.url, config);
    return Promise.reject(new Error(`Legacy CMS client does not support ${request.method}`));
  },
};

/** @deprecated Inject a Mango HttpClient and call createCmsApi() for instance isolation. */
export const cmsApi = createCmsApi(legacyHttpClient);
