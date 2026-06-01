import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';

export interface UserQuery {
  pageNum?: number;
  pageSize?: number;
  username?: string;
  nickname?: string;
  phone?: string;
  email?: string;
  status?: number;
  realm?: string;
  actorType?: string;
  partyType?: string;
  partyId?: ApiId;
  orgId?: ApiId;
}

export interface IdentityUserVO {
  userId?: ApiId;
  memberId?: ApiId;
  memberName?: string;
  memberType?: string;
  username: string;
  password?: string;
  nickname?: string;
  realm?: string;
  actorType?: string;
  partyType?: string;
  partyId?: ApiId;
  email?: string;
  phone?: string;
  avatar?: string;
  status?: number;
  memberStatus?: number;
  primaryOrgId?: ApiId;
  orgRelationId?: ApiId;
  orgId?: ApiId;
  postId?: ApiId;
  postName?: string;
  postCode?: string;
  primaryOrgFlag?: boolean;
  orgLeaderFlag?: boolean;
  tenantId?: string;
  lastLoginTime?: string;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

type CreateUserCommand = Pick<IdentityUserVO,
  'username' | 'password' | 'nickname' | 'realm' | 'actorType' | 'partyType' | 'partyId' |
  'email' | 'phone' | 'avatar' | 'status' | 'remark'
>;

type UpdateUserCommand = Pick<IdentityUserVO,
  'userId' | 'nickname' | 'partyType' | 'partyId' | 'email' | 'phone' | 'avatar' | 'status' | 'remark'
>;

interface BackendPageResult<T> {
  records?: T[];
  list?: T[];
  total?: number | string;
  current?: number | string;
  page?: number | string;
  size?: number | string;
  pageNum?: number | string;
  pageSize?: number | string;
}

export const userApi = {
  page(params?: UserQuery) {
    return get<BackendPageResult<IdentityUserVO>>('/identity/users/page', { params: toBackendQuery(params) })
      .then((data) => toPageResult(data, params));
  },
  detail: (userId: ApiId) => get<IdentityUserVO>('/identity/users/detail', { params: { userId } }),
  create: (data: IdentityUserVO) => post<ApiId>('/identity/users', toCreateCommand(data)),
  update: (data: IdentityUserVO) => put<boolean>('/identity/users', toUpdateCommand(data)),
  delete: (userId: ApiId) => del<boolean>('/identity/users', { params: { userId } }),
  updateStatus: (userId: ApiId, status: number) => put<boolean>('/identity/users/status', { userId, status }),
  resetPassword: (userId: ApiId, password: string) => put<boolean>('/identity/users/password/reset', { userId, password }),
};

function toBackendQuery(params?: UserQuery) {
  return {
    page: params?.pageNum,
    size: params?.pageSize,
    username: params?.username,
    nickname: params?.nickname,
    phone: params?.phone,
    email: params?.email,
    status: params?.status,
    realm: params?.realm,
    actorType: params?.actorType,
    partyType: params?.partyType,
    partyId: params?.partyId,
    orgId: params?.orgId,
  };
}

function toCreateCommand(data: IdentityUserVO): CreateUserCommand {
  return {
    username: data.username,
    password: data.password,
    nickname: data.nickname,
    realm: data.realm,
    actorType: data.actorType,
    partyType: data.partyType,
    partyId: data.partyId,
    email: data.email,
    phone: data.phone,
    avatar: data.avatar,
    status: data.status,
    remark: data.remark,
  };
}

function toUpdateCommand(data: IdentityUserVO): UpdateUserCommand {
  return {
    userId: data.userId,
    nickname: data.nickname,
    partyType: data.partyType,
    partyId: data.partyId,
    email: data.email,
    phone: data.phone,
    avatar: data.avatar,
    status: data.status,
    remark: data.remark,
  };
}

function toPageResult<T>(data?: BackendPageResult<T>, params?: UserQuery): PageResult<T> {
  const list = data?.records || data?.list || [];
  return {
    list,
    total: toFiniteNumber(data?.total, list.length),
    pageNum: toFiniteNumber(data?.current ?? data?.page ?? data?.pageNum, params?.pageNum ?? 1),
    pageSize: toFiniteNumber(data?.size ?? data?.pageSize, params?.pageSize ?? 10),
  };
}

function toFiniteNumber(value: number | string | undefined, fallback: number): number {
  const numberValue = Number(value ?? fallback);
  return Number.isFinite(numberValue) ? numberValue : fallback;
}
