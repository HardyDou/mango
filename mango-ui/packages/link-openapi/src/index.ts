export interface LinkPublicItem {
  id?: string;
  categoryId?: string;
  categoryName?: string;
  name?: string;
  url?: string;
  summary?: string;
  iconUrl?: string;
  tags?: string[];
  openMode?: 'NEW_WINDOW';
  recommended?: boolean;
  sortNo?: number;
  favorited?: boolean;
  source?: 'PUBLIC' | 'COMPANY' | 'FAVORITE' | 'PERSONAL';
  redirectUrl?: string;
}

export interface LinkPublicItemQuery {
  tenantId?: string;
  categoryId?: string;
  keyword?: string;
}

export interface LinkCategory {
  id?: string;
  name?: string;
  scope?: 'COMPANY' | 'PERSONAL';
  ownerUserId?: string;
  sortNo?: number;
  status?: 'ENABLED' | 'DISABLED';
}

export interface CreateLinkPersonalCategoryInput {
  name: string;
  sortNo?: number;
}

export interface CreateLinkPersonalItemInput {
  name: string;
  url: string;
  categoryId?: string;
  summary?: string;
  iconUrl?: string;
  tags?: string[];
  remark?: string;
}

export interface LinkOpenApiClientOptions {
  baseUrl?: string;
  headers?: HeadersInit | (() => HeadersInit | Promise<HeadersInit>);
  credentials?: RequestCredentials;
  fetcher?: typeof fetch;
}

interface MangoResult<T> {
  code?: number | string;
  success?: boolean;
  msg?: string;
  message?: string;
  data?: T;
}

const publicLinksPath = '/link/open/public-links/list';
const personalCategoriesPath = '/link/personal-categories';
const personalLinksPath = '/link/personal-links';
const favoritesPath = '/link/favorites';

function trimTrailingSlash(value: string) {
  return value.replace(/\/+$/, '');
}

function appendQuery(url: URL, query: LinkPublicItemQuery) {
  Object.entries(query).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    url.searchParams.set(key, String(value));
  });
}

function isSuccessResult(result: MangoResult<unknown>) {
  return result.success === true || result.code === 0 || result.code === 200 || result.code === '0' || result.code === '200';
}

async function resolveHeaders(options: LinkOpenApiClientOptions) {
  if (typeof options.headers === 'function') {
    return options.headers();
  }
  return options.headers;
}

async function mergedHeaders(options: LinkOpenApiClientOptions, headers?: HeadersInit) {
  const result = new Headers(await resolveHeaders(options));
  if (headers) {
    new Headers(headers).forEach((value, key) => result.set(key, value));
  }
  return result;
}

async function requestJson<T>(path: string, init: RequestInit = {}, options: LinkOpenApiClientOptions = {}) {
  const fetcher = options.fetcher || globalThis.fetch;
  if (!fetcher) {
    throw new Error('当前环境不支持 fetch，请通过 options.fetcher 传入请求实现');
  }
  const baseUrl = trimTrailingSlash(options.baseUrl || globalThis.location?.origin || '');
  const response = await fetcher(`${baseUrl}${path}`, {
    ...init,
    credentials: options.credentials,
    headers: await mergedHeaders(options, init.headers),
  });
  if (!response.ok) {
    throw new Error(`网址接口请求失败：${response.status}`);
  }
  const result = await response.json() as MangoResult<T>;
  if (!isSuccessResult(result)) {
    throw new Error(result.message || result.msg || '网址接口返回失败');
  }
  return result.data as T;
}

export async function listPublicLinks(query: LinkPublicItemQuery = {}, options: LinkOpenApiClientOptions = {}) {
  const baseUrl = trimTrailingSlash(options.baseUrl || globalThis.location?.origin || '');
  const url = new URL(`${baseUrl}${publicLinksPath}`);
  appendQuery(url, query);
  return requestJson<LinkPublicItem[]>(`${url.pathname}${url.search}`, { method: 'GET' }, options).then((data) => data || []);
}

export async function listPersonalCategories(options: LinkOpenApiClientOptions = {}) {
  return requestJson<LinkCategory[]>(`${personalCategoriesPath}/list`, { method: 'GET' }, options).then((data) => data || []);
}

export async function createPersonalCategory(input: CreateLinkPersonalCategoryInput, options: LinkOpenApiClientOptions = {}) {
  return requestJson<string>(`${personalCategoriesPath}/create`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }, options);
}

export async function createPersonalLink(input: CreateLinkPersonalItemInput, options: LinkOpenApiClientOptions = {}) {
  return requestJson<string>(`${personalLinksPath}/create`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(input),
  }, options);
}

export async function createFavorite(linkId: string, options: LinkOpenApiClientOptions = {}) {
  return requestJson<boolean>(`${favoritesPath}/create`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ linkId }),
  }, options);
}

export async function deleteFavorite(linkId: string, options: LinkOpenApiClientOptions = {}) {
  return requestJson<boolean>(`${favoritesPath}/delete`, {
    method: 'DELETE',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ linkId }),
  }, options);
}

export function createLinkOpenApiClient(options: LinkOpenApiClientOptions = {}) {
  return {
    listPublicLinks: (query: LinkPublicItemQuery = {}) => listPublicLinks(query, options),
    listPersonalCategories: () => listPersonalCategories(options),
    createPersonalCategory: (input: CreateLinkPersonalCategoryInput) => createPersonalCategory(input, options),
    createPersonalLink: (input: CreateLinkPersonalItemInput) => createPersonalLink(input, options),
    createFavorite: (linkId: string) => createFavorite(linkId, options),
    deleteFavorite: (linkId: string) => deleteFavorite(linkId, options),
  };
}
