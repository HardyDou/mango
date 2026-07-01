import { del, get, post, put } from '@mango/common/utils/request';
import type { LinkPublicItem, LinkPublicItemQuery } from '@mango/link-openapi';

export type ApiId = string;
export type LinkStatus = 'ENABLED' | 'DISABLED';
export type LinkVisibilityScope = 'PUBLIC' | 'COMPANY' | 'DEPARTMENT' | 'USER' | 'PERSONAL';
export type LinkVisibilityTargetType = 'DEPARTMENT' | 'USER';
export type LinkOpenMode = 'NEW_WINDOW';
export type LinkNavigationSource = NonNullable<LinkPublicItem['source']>;

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages?: number;
}

type BackendPageResult<T> = {
  list?: T[];
  total?: number;
  page?: number;
  size?: number;
  pages?: number;
};

export interface LinkCategory {
  id?: ApiId;
  tenantId?: ApiId;
  parentId?: ApiId | '';
  name?: string;
  code?: string;
  summary?: string;
  sortNo?: number;
  status?: LinkStatus;
  createTime?: string;
  updateTime?: string;
  children?: LinkCategory[];
}

export interface LinkVisibilityTarget {
  targetType?: LinkVisibilityTargetType;
  targetId?: ApiId;
  targetName?: string;
}

export interface LinkItem extends LinkPublicItem {
  tenantId?: ApiId;
  visibilityScope?: LinkVisibilityScope;
  visibilityTargets?: LinkVisibilityTarget[];
  ownerUserId?: ApiId;
  status?: LinkStatus;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

export interface LinkFavorite extends LinkPublicItem {
  favoriteTime?: string;
}

export interface LinkPersonalItem extends LinkPublicItem {
  status?: LinkStatus;
  createTime?: string;
  updateTime?: string;
}

export interface LinkPageQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  categoryId?: ApiId | '';
  visibilityScope?: LinkVisibilityScope | '';
  status?: LinkStatus | '';
}

export interface LinkListQuery {
  keyword?: string;
  categoryId?: ApiId | '';
  includeDisabled?: boolean;
  status?: LinkStatus | '';
}

export function normalizeApiId(id: unknown): ApiId | undefined {
  if (id === undefined || id === null || id === '') {
    return undefined;
  }
  return String(id);
}

function requestErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

function toBackendParams(params: object = {}) {
  const next: Record<string, unknown> = {};
  Object.entries(params as Record<string, unknown>).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    if (key === 'pageNum') {
      next.page = value;
      return;
    }
    if (key === 'pageSize') {
      next.size = value;
      return;
    }
    next[key] = value;
  });
  return next;
}

function normalizePageResult<T>(result: BackendPageResult<T>): PageResult<T> {
  return {
    list: result.list || [],
    total: Number(result.total || 0),
    pageNum: Number(result.page || 1),
    pageSize: Number(result.size || 10),
    pages: Number(result.pages || 0),
  };
}

function pageGet<T>(url: string, params: LinkPageQuery) {
  return get<BackendPageResult<T>>(url, { params: toBackendParams(params) }).then(normalizePageResult);
}

export function linkRedirectUrl(linkId: ApiId, source: LinkNavigationSource = 'COMPANY') {
  return `/api/link/open/redirect/${encodeURIComponent(linkId)}?source=${encodeURIComponent(source)}`;
}

export function openLinkWithRedirect(item: Pick<LinkPublicItem, 'id' | 'url'>, source: LinkNavigationSource = 'COMPANY') {
  const id = normalizeApiId(item.id);
  const target = id ? linkRedirectUrl(id, source) : item.url;
  if (!target) {
    return;
  }
  window.open(target, '_blank', 'noopener,noreferrer');
}

export function navigationSourceOf(scope?: LinkVisibilityScope): LinkNavigationSource {
  if (scope === 'PUBLIC') {
    return 'PUBLIC';
  }
  if (scope === 'PERSONAL') {
    return 'PERSONAL';
  }
  return 'COMPANY';
}

export const linkApi = {
  pageCategories: (params: LinkPageQuery) => pageGet<LinkCategory>('/link/categories/page', params),
  listCategories: (params: LinkListQuery = {}) => get<LinkCategory[]>('/link/categories/list', { params: toBackendParams(params) }),
  createCategory: (data: LinkCategory) => post<ApiId>('/link/categories/create', data),
  updateCategory: (data: LinkCategory) => put<boolean>('/link/categories/update', data),
  enableCategory: (id: ApiId) => post<boolean>('/link/categories/enable', undefined, { params: { id } }),
  disableCategory: (id: ApiId) => post<boolean>('/link/categories/disable', undefined, { params: { id } }),
  deleteCategory: (id: ApiId) => del<boolean>('/link/categories/delete', { params: { id } }),

  pageItems: (params: LinkPageQuery) => pageGet<LinkItem>('/link/items/page', params),
  createItem: (data: LinkItem) => post<ApiId>('/link/items/create', data),
  updateItem: (data: LinkItem) => put<boolean>('/link/items/update', data),
  enableItem: (id: ApiId) => post<boolean>('/link/items/enable', undefined, { params: { id } }),
  disableItem: (id: ApiId) => post<boolean>('/link/items/disable', undefined, { params: { id } }),
  deleteItem: (id: ApiId) => del<boolean>('/link/items/delete', { params: { id } }),

  listCompanyLinks: (params: LinkPublicItemQuery = {}) => get<LinkItem[]>('/link/company-links/list', { params: toBackendParams(params) }),
  createFavorite: (linkId: ApiId) => post<boolean>('/link/favorites/create', { linkId }),
  deleteFavorite: (linkId: ApiId) => del<boolean>('/link/favorites/delete', { data: { linkId } }),
  listFavorites: (params: LinkListQuery = {}) => get<LinkFavorite[]>('/link/favorites/list', { params: toBackendParams(params) }),

  pagePersonalItems: (params: LinkPageQuery) => pageGet<LinkPersonalItem>('/link/personal-links/page', params),
  createPersonalItem: (data: LinkPersonalItem) => post<ApiId>('/link/personal-links/create', data),
  updatePersonalItem: (data: LinkPersonalItem) => put<boolean>('/link/personal-links/update', data),
  deletePersonalItem: (id: ApiId) => del<boolean>('/link/personal-links/delete', { params: { id } }),
};

export { requestErrorMessage };
