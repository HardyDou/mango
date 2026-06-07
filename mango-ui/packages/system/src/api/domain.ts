import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

export interface DomainItem {
  id?: ApiId;
  tenantId?: string;
  domainCode: string;
  domainShortCode: string;
  domainName: string;
  parentId: ApiId | 0;
  parentName?: string;
  sort: number;
  status: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
  createdBy?: ApiId;
  createdAt?: string;
  updatedBy?: ApiId;
  updatedAt?: string;
  children?: DomainItem[];
}

export interface DomainPageQuery {
  domainCode?: string;
  domainName?: string;
  status?: number;
  page?: number;
  size?: number;
}

export interface DomainPageResult {
  list: DomainItem[];
  total: number;
  page: number;
  size: number;
}

export interface CreateDomainPayload {
  domainCode: string;
  domainShortCode: string;
  domainName: string;
  parentId?: ApiId | 0;
  sort?: number;
  status?: number;
  remark?: string;
}

export interface UpdateDomainPayload {
  id: ApiId;
  domainShortCode: string;
  domainName: string;
  sort?: number;
  status?: number;
  remark?: string;
}

export const domainApi = {
  page(params?: DomainPageQuery) {
    return get<DomainPageResult>('/domain/domains/page', { params });
  },
  tree(params?: DomainPageQuery) {
    return get<DomainItem[]>('/domain/domains/tree', { params });
  },
  enabledTree() {
    return get<DomainItem[]>('/domain/domains/enabled-tree');
  },
  detail(id: ApiId) {
    return get<DomainItem>('/domain/domains/detail', { params: { id } });
  },
  detailByCode(domainCode: string) {
    return get<DomainItem>('/domain/domains/code', { params: { domainCode } });
  },
  create(data: CreateDomainPayload) {
    return post<ApiId>('/domain/domains', normalizeCreate(data));
  },
  update(data: UpdateDomainPayload) {
    return put<boolean>('/domain/domains', normalizeUpdate(data));
  },
  updateStatus(id: ApiId, status: number) {
    return put<boolean>('/domain/domains/status', { id, status });
  },
  delete(id: ApiId) {
    return del<boolean>('/domain/domains', { params: { id } });
  },
};

function normalizeCreate(data: CreateDomainPayload): CreateDomainPayload {
  return {
    ...data,
    domainCode: normalizeCode(data.domainCode),
    domainShortCode: normalizeCode(data.domainShortCode),
    parentId: data.parentId ?? 0,
    sort: data.sort ?? 0,
    status: data.status ?? 1,
    remark: data.remark?.trim() || '',
  };
}

function normalizeUpdate(data: UpdateDomainPayload): UpdateDomainPayload {
  return {
    ...data,
    domainShortCode: normalizeCode(data.domainShortCode),
    domainName: data.domainName.trim(),
    sort: data.sort ?? 0,
    status: data.status ?? 1,
    remark: data.remark?.trim() || '',
  };
}

export function normalizeCode(value: string) {
  return value.trim().replace(/-/g, '_').toUpperCase();
}
