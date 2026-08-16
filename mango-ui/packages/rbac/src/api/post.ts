import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

export interface PostVO {
  id?: ApiId;
  postName: string;
  postCode: string;
  postSort?: number;
  postStatus?: string;
  remark?: string;
  tenantId?: ApiId;
  createTime?: string;
  updateTime?: string;
}

export interface PostQuery {
  pageNum?: number;
  pageSize?: number;
  postName?: string;
  postCode?: string;
  postStatus?: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

interface BackendPageResult<T> {
  records?: T[];
  list?: T[];
  total?: number | string;
  current?: number | string;
  pageNum?: number | string;
  size?: number | string;
  pageSize?: number | string;
}

type PostCommand = Omit<PostVO, 'tenantId' | 'createTime' | 'updateTime'>;

export const postApi = {
  page: (params?: PostQuery) => {
    return get<BackendPageResult<PostVO>>('/post/page', { params: toBackendQuery(params) }).then((data) =>
      toPageResult(data, params),
    );
  },
  detail: (id: ApiId) => get<PostVO>('/post/detail', { params: { id } }),
  create: (data: PostVO) => post<ApiId>('/post', toBackendPost(data)),
  update: (data: PostVO) => put<boolean>('/post', toBackendPost(data)),
  delete: (id: ApiId) => del<boolean>('/post', { params: { id } }),
};

function toBackendQuery(params?: PostQuery) {
  return {
    page: params?.pageNum,
    size: params?.pageSize,
    postName: params?.postName,
    postCode: params?.postCode,
    postStatus: params?.postStatus,
  };
}

function toBackendPost(data: PostVO): PostCommand {
  return {
    id: data.id,
    postName: data.postName,
    postCode: data.postCode,
    postSort: data.postSort ?? 0,
    postStatus: data.postStatus ?? '1',
    remark: data.remark,
  };
}

function toPageResult<T>(data?: BackendPageResult<T>, params?: PostQuery): PageResult<T> {
  const list = data?.records || data?.list || [];
  return {
    list,
    total: Number(data?.total ?? list.length),
    pageNum: Number(data?.current ?? data?.pageNum ?? params?.pageNum ?? 1),
    pageSize: Number(data?.size ?? data?.pageSize ?? params?.pageSize ?? 10),
  };
}
